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

package katana.analysis;

import katana.ast.AstPath;
import katana.ast.decl.*;
import katana.ast.stmt.AstStmt;
import katana.backend.PlatformContext;
import katana.sema.SemaModule;
import katana.sema.SemaProgram;
import katana.sema.SemaSymbol;
import katana.sema.decl.*;
import katana.sema.scope.SemaScopeFile;
import katana.sema.stmt.SemaStmt;
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
	private SemaProgram program;
	private Consumer<SemaDecl> validateDecl;

	private IdentityHashMap<SemaDecl, AstDecl> decls = new IdentityHashMap<>();
	private IdentityHashMap<SemaDeclDefinedFunction, StmtValidator> validators = new IdentityHashMap<>();
	private Map<String, SemaDeclOverloadSet> overloadSets = new HashMap<>();
	private SemaModule currentModule = null;
	private boolean declsSeen = false;
	private Map<AstPath, AstDeclImport> imports = new HashMap<>();
	private Map<String, AstDeclRenamedImport> renamedImports = new HashMap<>();
	private SemaScopeFile scope = new SemaScopeFile();

	public FileValidator(PlatformContext context, SemaProgram program, Consumer<SemaDecl> validateDecl)
	{
		this.context = context;
		this.program = program;
		this.validateDecl = validateDecl;
	}

	public Maybe<SemaDecl> register(AstDecl decl)
	{
		return (Maybe<SemaDecl>)decl.accept(this);
	}

	private SemaModule checkModuleImport(AstPath path)
	{
		Maybe<SemaModule> module = program.findModule(path);

		if(module.isNone())
			throw new RuntimeException(String.format("import of unknown module '%s'", path));

		return module.unwrap();
	}

	public void validateImports()
	{
		for(AstDeclImport import_ : imports.values())
		{
			SemaModule module = checkModuleImport(import_.path);

			for(SemaDecl decl : module.decls().values())
				if(decl instanceof SemaDeclOverloadSet)
					scope.defineSymbol(new SemaDeclImportedOverloadSet((SemaDeclOverloadSet)decl));
				else if(decl.exported)
					scope.defineSymbol(decl);
		}

		for(AstDeclRenamedImport import_ : renamedImports.values())
		{
			SemaModule module = checkModuleImport(import_.path);
			SemaDeclRenamedImport semaImport = new SemaDeclRenamedImport(module, import_.rename);

			for(SemaDecl decl : module.decls().values())
				if(decl instanceof SemaDeclOverloadSet)
					semaImport.decls.put(decl.name(), new SemaDeclImportedOverloadSet((SemaDeclOverloadSet)decl));
				else
					semaImport.decls.put(decl.name(), decl);

			scope.defineSymbol(semaImport);
		}
	}

	public void validate(SemaDecl decl)
	{
		Map<SemaDeclDefinedFunction, StmtValidator> validators = DeclValidator.validate(decl, decls.get(decl), scope, context, validateDecl);

		if(!validators.isEmpty())
			this.validators.putAll(validators);
	}

	private void validate(SemaDeclDefinedFunction semaFunction, AstDeclDefinedFunction function, StmtValidator validator)
	{
		for(AstStmt stmt : function.body)
		{
			SemaStmt semaStmt = validator.validate(stmt);
			semaFunction.add(semaStmt);
		}

		validator.finalizeValidation();
	}

	public void finish()
	{
		for(SemaDeclOverloadSet set : overloadSets.values())
		{
			OverloadDeclList overloadDecls = (OverloadDeclList)decls.get(set);

			for(int i = 0; i != set.overloads.size(); ++i)
			{
				SemaDeclFunction overload = set.overloads.get(i);

				if(!(overload instanceof SemaDeclDefinedFunction))
					continue;

				SemaDeclDefinedFunction semaFunction = (SemaDeclDefinedFunction)overload;
				AstDeclDefinedFunction function = (AstDeclDefinedFunction)overloadDecls.decls.get(i);
				StmtValidator validator = validators.get(semaFunction);
				validate(semaFunction, function, validator);
			}
		}
	}

	private void requireModule()
	{
		if(currentModule == null)
			throw new RuntimeException("no module defined");
	}

	private void redefinitionError(SemaSymbol symbol)
	{
		throw new RuntimeException(String.format("redefinition of symbol '%s'", symbol.name()));
	}

	private Maybe<SemaDecl> handleModuleDecl(SemaDecl semaDecl, AstDecl decl)
	{
		declsSeen = true;
		requireModule();

		if(!currentModule.declare(semaDecl))
			redefinitionError(semaDecl);

		decls.put(semaDecl, decl);
		scope.defineSymbol(semaDecl);

		return Maybe.some(semaDecl);
	}

	private Maybe<SemaDecl> visit(AstDeclData data)
	{
		SemaDeclData semaData = new SemaDeclData(currentModule, data.exported, data.opaque, data.name);
		return handleModuleDecl(semaData, data);
	}

	private Maybe<SemaDecl> handleFunction(SemaDeclFunction semaFunction, AstDeclFunction function)
	{
		String name = semaFunction.name();
		SemaDeclOverloadSet set = overloadSets.get(name);

		if(set == null)
		{
			set = new SemaDeclOverloadSet(semaFunction.module(), name);
			set.overloads.add(semaFunction);
			overloadSets.put(name, set);
			OverloadDeclList list = new OverloadDeclList();
			list.decls.add(function);
			return handleModuleDecl(set, list);
		}

		set.overloads.add(semaFunction);
		((OverloadDeclList)decls.get(set)).decls.add(function);
		return Maybe.none();
	}

	private Maybe<SemaDecl> visit(AstDeclExternFunction function)
	{
		SemaDeclExternFunction semaFunction = new SemaDeclExternFunction(currentModule, function.exported, function.opaque, function.externName, function.name);
		return handleFunction(semaFunction, function);
	}

	private Maybe<SemaDecl> visit(AstDeclDefinedFunction function)
	{
		SemaDeclDefinedFunction semaFunction = new SemaDeclDefinedFunction(currentModule, function.exported, function.opaque, function.name);
		return handleFunction(semaFunction, function);
	}

	private Maybe<SemaDecl> visit(AstDeclGlobal global)
	{
		SemaDeclGlobal semaGlobal = new SemaDeclGlobal(currentModule, global.exported, global.opaque, global.name);
		return handleModuleDecl(semaGlobal, global);
	}

	private Maybe<SemaDecl> visit(AstDeclTypeAlias alias)
	{
		SemaDeclTypeAlias semaAlias = new SemaDeclTypeAlias(currentModule, alias.exported, alias.name);
		return handleModuleDecl(semaAlias, alias);
	}

	private Maybe<SemaDecl> visit(AstDeclImport import_)
	{
		if(declsSeen)
			throw new RuntimeException("imports must go before other decls");

		if(imports.containsKey(import_.path))
			throw new RuntimeException(String.format("duplicate import '%s'", import_.path));

		imports.put(import_.path, import_);

		return Maybe.none();
	}

	private Maybe<SemaDecl> visit(AstDeclRenamedImport import_)
	{
		if(declsSeen)
			throw new RuntimeException("imports must go before other decls");

		if(renamedImports.containsKey(import_.rename))
			throw new RuntimeException(String.format("duplicate renamed import '%s'", import_.rename));

		renamedImports.put(import_.rename, import_);

		return Maybe.none();
	}

	private Maybe<SemaDecl> visit(AstDeclModule module_)
	{
		declsSeen = true;
		currentModule = program.findOrCreateModule(module_.path);
		return Maybe.none();
	}
}
