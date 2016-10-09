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

import katana.ast.AstPath;
import katana.sema.decl.SemaDecl;
import katana.utils.Maybe;

import java.util.Map;
import java.util.TreeMap;

public class SemaModule implements SemaSymbol
{
	public SemaModule(String name, AstPath path, SemaModule parent)
	{
		this.name = name;
		this.path = path;
		this.parent = parent;
	}

	public boolean declare(SemaDecl decl)
	{
		if(decls.containsKey(decl.name()))
			return false;

		decls.put(decl.name(), decl);
		return true;
	}

	public Maybe<SemaDecl> findDecl(String name)
	{
		SemaModule current = this;
		SemaDecl decl;

		do
		{
			decl = decls.get(name);
			current = current.parent;
		}

		while(decl == null && current != null);

		return Maybe.wrap(decl);
	}

	public Maybe<SemaModule> findChild(String name)
	{
		SemaModule module = children.get(name);
		return Maybe.wrap(module);
	}

	public SemaModule findOrCreateChild(String name)
	{
		Maybe<SemaModule> child = findChild(name);

		if(child.isSome())
			return child.get();

		AstPath path = new AstPath();
		path.components.addAll(this.path.components);
		path.components.add(name);

		SemaModule module = new SemaModule(name, path, this);
		children.put(name, module);

		return module;
	}

	public Map<String, SemaModule> children()
	{
		return children;
	}

	public Map<String, SemaDecl> decls() { return decls; }

	public AstPath path()
	{
		return path;
	}

	@Override
	public String name()
	{
		return name;
	}

	private String name;
	private AstPath path;
	private SemaModule parent;
	private Map<String, SemaModule> children = new TreeMap<>();
	private Map<String, SemaDecl> decls = new TreeMap<>();
}
