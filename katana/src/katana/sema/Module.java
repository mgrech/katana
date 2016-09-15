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

	public boolean declare(Decl decl)
	{
		if(decls.containsKey(decl.name()))
			return false;

		decls.put(decl.name(), decl);
		return true;
	}

	public Maybe<Decl> findDecl(String name)
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
}
