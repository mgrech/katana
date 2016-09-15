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
import katana.sema.decl.*;
import katana.utils.Maybe;

import java.util.Map;
import java.util.TreeMap;

public class Module implements Symbol
{
	public Module(String name, Path path, Module parent)
	{
		this.name = name;
		this.path = path;
		this.parent = parent;
	}

	public boolean defineData(Data data)
	{
		if(!defineSymbol(data))
			return false;

		datas.put(data.name(), data);
		return true;
	}

	public boolean defineGlobal(Global global)
	{
		if(!defineSymbol(global))
			return false;

		globals.put(global.name, global);
		return true;
	}

	public boolean defineFunction(Function function)
	{
		if(!defineSymbol(function))
			return false;

		functions.put(function.name(), function);
		return true;
	}

	public boolean defineExternFunction(ExternFunction function)
	{
		if(!defineSymbol(function))
			return false;

		externFunctions.put(function.name(), function);
		return true;
	}

	public boolean defineTypeAlias(TypeAlias alias)
	{
		if(!defineSymbol(alias))
			return false;

		aliases.put(alias.name(), alias);
		return true;
	}

	private boolean defineSymbol(Decl decl)
	{
		if(decls.containsKey(decl.name()))
			return false;

		decls.put(decl.name(), decl);
		return true;
	}

	public Maybe<Decl> findSymbol(String name)
	{
		Module current = this;
		Decl decl;

		do
		{
			decl = decls.get(name);
			current = current.parent;
		}

		while(decl == null && current != null);

		return Maybe.wrap(decl);
	}

	public Maybe<Module> findChild(String name)
	{
		Module module = children.get(name);
		return Maybe.wrap(module);
	}

	public Module findOrCreateChild(String name)
	{
		Maybe<Module> child = findChild(name);

		if(child.isSome())
			return child.get();

		Path path = new Path();
		path.components.addAll(this.path.components);
		path.components.add(name);

		Module module = new Module(name, path, this);
		children.put(name, module);

		return module;
	}

	public Map<String, Module> children()
	{
		return children;
	}

	public Map<String, Decl> decls() { return decls; }

	public Map<String, Data> datas()
	{
		return datas;
	}

	public Map<String, Global> globals()
	{
		return globals;
	}

	public Map<String, Function> functions()
	{
		return functions;
	}

	public Map<String, ExternFunction> externFunctions()
	{
		return externFunctions;
	}

	public Path path()
	{
		return path;
	}

	@Override
	public String name()
	{
		return name;
	}

	private String name;
	private Path path;
	private Module parent;
	private Map<String, Module> children = new TreeMap<>();

	private Map<String, Decl> decls = new TreeMap<>();
	private Map<String, Data> datas = new TreeMap<>();
	private Map<String, Global> globals = new TreeMap<>();
	private Map<String, Function> functions = new TreeMap<>();
	private Map<String, ExternFunction> externFunctions = new TreeMap<>();
	private Map<String, TypeAlias> aliases = new TreeMap<>();
}
