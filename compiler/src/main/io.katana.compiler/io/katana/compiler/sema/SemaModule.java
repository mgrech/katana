// Copyright 2016-2019 Markus Grech
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

package io.katana.compiler.sema;

import io.katana.compiler.ast.AstPath;
import io.katana.compiler.sema.decl.SemaDecl;
import io.katana.compiler.utils.Maybe;

import java.util.Map;
import java.util.TreeMap;

public class SemaModule implements SemaSymbol
{
	private String name;
	private AstPath path;
	private SemaModule parent;
	private Map<String, SemaModule> childrenByName = new TreeMap<>();
	private Map<String, SemaDecl> declsByName = new TreeMap<>();

	public SemaModule(String name, AstPath path, SemaModule parent)
	{
		this.name = name;
		this.path = path;
		this.parent = parent;
	}

	public boolean declare(SemaDecl decl)
	{
		if(declsByName.containsKey(decl.name()))
			return false;

		declsByName.put(decl.name(), decl);
		return true;
	}

	public Maybe<SemaDecl> findDecl(String name)
	{
		var current = this;
		SemaDecl decl;

		do
		{
			decl = declsByName.get(name);
			current = current.parent;
		}
		while(decl == null && current != null);

		return Maybe.wrap(decl);
	}

	public Maybe<SemaModule> findChild(String name)
	{
		var module = childrenByName.get(name);
		return Maybe.wrap(module);
	}

	public SemaModule findOrCreateChild(String name)
	{
		var child = findChild(name);

		if(child.isSome())
			return child.get();

		var path = new AstPath();
		path.components.addAll(this.path.components);
		path.components.add(name);

		var module = new SemaModule(name, path, this);
		childrenByName.put(name, module);

		return module;
	}

	public Map<String, SemaModule> children()
	{
		return childrenByName;
	}

	public Map<String, SemaDecl> decls() { return declsByName; }

	public AstPath path()
	{
		return path;
	}

	@Override
	public String name()
	{
		return name;
	}
}
