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

package katana.sema.decl;

import katana.sema.Module;
import katana.sema.Symbol;
import katana.sema.stmt.Label;
import katana.sema.stmt.Stmt;
import katana.sema.type.Type;
import katana.utils.Maybe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Function extends Decl
{
	public class Param implements Symbol
	{
		public Param(String name, Type type, int index)
		{
			this.name = name;
			this.type = type;
			this.index = index;
		}

		@Override
		public String name()
		{
			return name;
		}

		public String name;
		public Type type;
		public int index;
	}

	public class Local implements Symbol
	{
		public Local(String name, Type type, int index)
		{
			this.name = name;
			this.type = type;
			this.index = index;
		}

		@Override
		public String name()
		{
			return name;
		}

		public String name;
		public Type type;
		public int index;
	}

	public Function(Module module, boolean exported, boolean opaque, String name)
	{
		super(module, exported, opaque);
		this.name = name;
	}

	public boolean defineParam(String name, Type type)
	{
		if(paramsByName.containsKey(name))
			return false;

		Param param = new Param(name, type, params.size());
		params.add(param);
		paramsByName.put(name, param);
		return true;
	}

	public boolean defineLocal(String name, Type type)
	{
		if(localsByName.containsKey(name))
			return false;

		Local local = new Local(name, type, locals.size());
		locals.add(local);
		localsByName.put(name, local);
		return true;
	}

	public boolean defineLabel(Label label)
	{
		if(labels.containsKey(label.name))
			return false;

		labels.put(label.name, label);
		return true;
	}

	public Maybe<Param> findParam(String name)
	{
		Param param = paramsByName.get(name);
		return Maybe.wrap(param);
	}

	public Maybe<Local> findLocal(String name)
	{
		Local local = localsByName.get(name);
		return Maybe.wrap(local);
	}

	public void add(Stmt stmt)
	{
		body.add(stmt);
	}

	@Override
	public String name()
	{
		return name;
	}

	private String name;
	public List<Param> params = new ArrayList<>();
	public Map<String, Param> paramsByName = new TreeMap<>();
	public Maybe<Type> ret;

	public List<Local> locals = new ArrayList<>();
	public Map<String, Local> localsByName = new TreeMap<>();
	public List<Stmt> body = new ArrayList<>();
	public Map<String, Label> labels = new TreeMap<>();
}
