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
import katana.backend.PlatformContext;
import katana.sema.decl.*;
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
	private Module currentModule = null;
	private boolean declsSeen = false;
	private Map<Path, Import> imports = new HashMap<>();
	private FileScope scope = new FileScope();

	public FileValidator(PlatformContext context, Program program, Consumer<Decl> validateDecl)
	{
		this.context = context;
		this.program = program;
		this.validateDecl = validateDecl;
	}

	public Maybe<Decl> beginValidation(katana.ast.decl.Decl decl)
	{
		return (Maybe<Decl>)decl.accept(this);
	}

	public void validateImports()
	{
		for(Import import_ : imports.values())
		{
			Maybe<Module> module = program.findModule(import_.path);

			if(module.isNone())
				throw new RuntimeException(String.format("import of unknown module '%s'", import_.path));

			if(import_.rename.isNone())
			{
				for(Decl decl : module.unwrap().decls().values())
					scope.defineSymbol(decl);
			}
		}
	}

	public void finalizeValidation(Decl decl)
	{
		DeclValidator.validate(decl, decls.get(decl), scope, context, validateDecl);
	}

	private void requireModule()
	{
		if(currentModule == null)
			throw new RuntimeException("no module defined");
	}

	private Maybe<Decl> visit(katana.ast.decl.Data data)
	{
		declsSeen = true;
		requireModule();

		Data semaData = new Data(currentModule, data.exported, data.opaque, data.name);

		if(!currentModule.defineData(semaData))
			throw new RuntimeException("redefinition of symbol '" + data.name + "'");

		decls.put(semaData, data);
		scope.defineSymbol(semaData);

		return Maybe.some(semaData);
	}

	private Maybe<Decl> visit(katana.ast.decl.ExternFunction function)
	{
		declsSeen = true;
		requireModule();

		ExternFunction semaFunction = new ExternFunction(currentModule, function.exported, function.opaque, function.externName, function.name);

		if(!currentModule.defineExternFunction(semaFunction))
			throw new RuntimeException("redefinition of symbol '" + function.name + "'");

		decls.put(semaFunction, function);
		scope.defineSymbol(semaFunction);

		return Maybe.some(semaFunction);
	}

	private Maybe<Decl> visit(katana.ast.decl.Function function)
	{
		declsSeen = true;
		requireModule();

		Function semaFunction = new Function(currentModule, function.exported, function.opaque, function.name);

		if(!currentModule.defineFunction(semaFunction))
			throw new RuntimeException("redefinition of symbol '" + function.name + "'");

		decls.put(semaFunction, function);
		scope.defineSymbol(semaFunction);

		return Maybe.some(semaFunction);
	}

	private Maybe<Decl> visit(katana.ast.decl.Global global)
	{
		declsSeen = true;
		requireModule();

		Global semaGlobal = new Global(currentModule, global.exported, global.opaque, global.name);

		if(!currentModule.defineGlobal(semaGlobal))
			throw new RuntimeException("redefinition of symbol '" + global.name + "'");

		decls.put(semaGlobal, global);
		scope.defineSymbol(semaGlobal);

		return Maybe.some(semaGlobal);
	}

	private Maybe<Decl> visit(katana.ast.decl.Import import_)
	{
		if(declsSeen)
			throw new RuntimeException("imports must go before other decls");

		if(imports.containsKey(import_.path))
			throw new RuntimeException("duplicate import '" + import_.path + "'");

		imports.put(import_.path, import_);

		return Maybe.none();
	}

	private Maybe<Decl> visit(katana.ast.decl.Module module_)
	{
		declsSeen = true;
		currentModule = program.findOrCreateModule(module_.path);
		return Maybe.none();
	}
}
