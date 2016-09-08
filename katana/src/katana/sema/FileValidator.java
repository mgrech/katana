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

import katana.ast.File;
import katana.ast.Path;
import katana.backend.PlatformContext;
import katana.sema.decl.Data;
import katana.sema.decl.ExternFunction;
import katana.sema.decl.Function;
import katana.sema.decl.Global;
import katana.visitor.IVisitor;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class FileValidator implements IVisitor
{
	private IdentityHashMap<Data, katana.ast.decl.Data> datas = new IdentityHashMap<>();
	private IdentityHashMap<Function, katana.ast.decl.Function> funcs = new IdentityHashMap<>();
	private IdentityHashMap<ExternFunction, katana.ast.decl.ExternFunction> extfuncs = new IdentityHashMap<>();
	private IdentityHashMap<Global, katana.ast.decl.Global> globals = new IdentityHashMap<>();

	private PlatformContext context;
	private Program program;
	private Module currentModule = null;
	private boolean declsSeen = false;
	private Set<Path> imports = new HashSet<>();

	public FileValidator(PlatformContext context, Program program)
	{
		this.context = context;
		this.program = program;
	}

	public void validate(File file)
	{
		for(katana.ast.Decl decl : file.decls)
			decl.accept(this);
	}

	public void finalizeValidation()
	{
		doFinalize(datas);
		doFinalize(globals);
		doFinalize(extfuncs);
		doFinalize(funcs);
	}

	public Set<Path> imports()
	{
		return imports;
	}

	private void requireModule()
	{
		if(currentModule == null)
			throw new RuntimeException("no module defined");
	}

	private <T extends Decl, U extends katana.ast.Decl>
	void doFinalize(IdentityHashMap<T, U> decls)
	{
		for(Map.Entry<T, U> entry : decls.entrySet())
		{
			T semaDecl = entry.getKey();
			U decl = entry.getValue();
			DeclValidator.validate(semaDecl, decl, currentModule, context);
		}
	}

	private void visit(katana.ast.decl.Data data)
	{
		declsSeen = true;
		requireModule();

		Data semaData = new Data(currentModule, data.name);

		if(!currentModule.defineData(semaData))
			throw new RuntimeException("redefinition of symbol '" + data.name + "'");

		datas.put(semaData, data);
	}

	private void visit(katana.ast.decl.ExternFunction function)
	{
		declsSeen = true;
		requireModule();

		ExternFunction semaFunction = new ExternFunction(currentModule, function.externName, function.name);

		if(!currentModule.defineExternFunction(semaFunction))
			throw new RuntimeException("redefinition of symbol '" + function.name + "'");

		extfuncs.put(semaFunction, function);
	}

	private void visit(katana.ast.decl.Function function)
	{
		declsSeen = true;
		requireModule();

		Function semaFunction = new Function(currentModule, function.name);

		if(!currentModule.defineFunction(semaFunction))
			throw new RuntimeException("redefinition of symbol '" + function.name + "'");

		funcs.put(semaFunction, function);
	}

	private void visit(katana.ast.decl.Global global)
	{
		declsSeen = true;
		requireModule();

		Global semaGlobal = new Global(currentModule, global.name);

		if(!currentModule.defineGlobal(semaGlobal))
			throw new RuntimeException("redefinition of symbol '" + global.name + "'");

		globals.put(semaGlobal, global);
	}

	private void visit(katana.ast.decl.Import import_)
	{
		if(declsSeen)
			throw new RuntimeException("imports must go before other decls");

		if(!imports.add(import_.path))
			throw new RuntimeException("duplicate import '" + import_.path + "'");
	}

	private void visit(katana.ast.decl.Module module_)
	{
		declsSeen = true;
		currentModule = program.findOrCreateModule(module_.path);
	}
}
