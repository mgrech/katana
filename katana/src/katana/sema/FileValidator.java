// Copyright 2016 Markus Grech
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package katana.sema;

import katana.ast.Path;
import katana.ast.decl.Import;
import katana.ast.decl.RenamedImport;
import katana.backend.PlatformContext;
import katana.sema.decl.*;
import katana.sema.stmt.Stmt;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class FileValidator implements IVisitor
{
	private PlatformContext context;
	private Program program;
	private Consumer<Decl> validateDecl;

	private IdentityHashMap<Decl, katana.ast.decl.Decl> decls = new IdentityHashMap<>();
	private IdentityHashMap<Function, StmtValidator> funcs = new IdentityHashMap<>();
	private Module currentModule = null;
	private boolean declsSeen = false;
	private Map<Path, Import> imports = new HashMap<>();
	private Map<String, RenamedImport> renamedImports = new HashMap<>();
	private FileScope scope = new FileScope();

	public FileValidator(PlatformContext context, Program program, Consumer<Decl> validateDecl)
	{
		this.context = context;
		this.program = program;
		this.validateDecl = validateDecl;
	}

	public Maybe<Decl> register(katana.ast.decl.Decl decl)
	{
		return (Maybe<Decl>)decl.accept(this);
	}

	private Module checkModuleImport(Path path)
	{
		Maybe<Module> module = program.findModule(path);

		if(module.isNone())
			throw new RuntimeException(String.format("import of unknown module '%s'", path));

		return module.unwrap();
	}

	public void validateImports()
	{
		for(Import import_ : imports.values())
		{
			Module module = checkModuleImport(import_.path);

			for(Decl decl : module.decls().values())
				if(decl.exported)
					scope.defineSymbol(decl);
		}

		for(RenamedImport import_ : renamedImports.values())
		{
			Module module = checkModuleImport(import_.path);
			katana.sema.decl.RenamedImport semaImport = new katana.sema.decl.RenamedImport(module, import_.rename);

			for(Decl decl : module.decls().values())
				semaImport.decls.put(decl.name(), decl);

			scope.defineSymbol(semaImport);
		}
	}

	public void validate(Decl decl)
	{
		Maybe<StmtValidator> validator = DeclValidator.validate(decl, decls.get(decl), scope, context, validateDecl);

		if(decl instanceof Function)
			funcs.put((Function)decl, validator.unwrap());
	}

	public void finish()
	{
		for(Map.Entry<Function, StmtValidator> entry : funcs.entrySet())
		{
			Function semaFunction = entry.getKey();
			katana.ast.decl.Function function = (katana.ast.decl.Function)decls.get(semaFunction);
			StmtValidator validator = entry.getValue();

			for(katana.ast.stmt.Stmt stmt : function.body)
			{
				Stmt semaStmt = validator.validate(stmt);
				semaFunction.add(semaStmt);
			}

			validator.finalizeValidation();
		}
	}

	private void requireModule()
	{
		if(currentModule == null)
			throw new RuntimeException("no module defined");
	}

	private void redefinitionError(Symbol symbol)
	{
		throw new RuntimeException(String.format("redefinition of symbol '%s'", symbol.name()));
	}

	private Maybe<Decl> handleModuleDecl(Decl semaDecl, katana.ast.decl.Decl decl)
	{
		declsSeen = true;
		requireModule();

		if(!currentModule.declare(semaDecl))
			redefinitionError(semaDecl);

		decls.put(semaDecl, decl);
		scope.defineSymbol(semaDecl);

		return Maybe.some(semaDecl);
	}

	private Maybe<Decl> visit(katana.ast.decl.Data data)
	{
		Data semaData = new Data(currentModule, data.exported, data.opaque, data.name);
		return handleModuleDecl(semaData, data);
	}

	private Maybe<Decl> visit(katana.ast.decl.ExternFunction function)
	{
		ExternFunction semaFunction = new ExternFunction(currentModule, function.exported, function.opaque, function.externName, function.name);
		return handleModuleDecl(semaFunction, function);
	}

	private Maybe<Decl> visit(katana.ast.decl.Function function)
	{
		Function semaFunction = new Function(currentModule, function.exported, function.opaque, function.name);
		return handleModuleDecl(semaFunction, function);
	}

	private Maybe<Decl> visit(katana.ast.decl.Global global)
	{
		Global semaGlobal = new Global(currentModule, global.exported, global.opaque, global.name);
		return handleModuleDecl(semaGlobal, global);
	}

	private Maybe<Decl> visit(katana.ast.decl.TypeAlias alias)
	{
		TypeAlias semaAlias = new TypeAlias(currentModule, alias.exported, alias.name);
		return handleModuleDecl(semaAlias, alias);
	}

	private Maybe<Decl> visit(katana.ast.decl.Import import_)
	{
		if(declsSeen)
			throw new RuntimeException("imports must go before other decls");

		if(imports.containsKey(import_.path))
			throw new RuntimeException(String.format("duplicate import '%s'", import_.path));

		imports.put(import_.path, import_);

		return Maybe.none();
	}

	private Maybe<Decl> visit(katana.ast.decl.RenamedImport import_)
	{
		if(declsSeen)
			throw new RuntimeException("imports must go before other decls");

		if(renamedImports.containsKey(import_.rename))
			throw new RuntimeException(String.format("duplicate renamed import '%s'", import_.rename));

		renamedImports.put(import_.rename, import_);

		return Maybe.none();
	}

	private Maybe<Decl> visit(katana.ast.decl.Module module_)
	{
		declsSeen = true;
		currentModule = program.findOrCreateModule(module_.path);
		return Maybe.none();
	}
}
