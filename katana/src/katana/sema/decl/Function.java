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
import katana.sema.type.Type;
import katana.utils.Maybe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class Function extends Decl
{
	public class Param implements Symbol
	{
		public Param(Type type, String name, int index)
		{
			this.type = type;
			this.name = name;
			this.index = index;
		}

		@Override
		public String name()
		{
			return name;
		}

		public Type type;
		public String name;
		public int index;
	}

	protected Function(Module module, boolean exported, boolean opaque, String name)
	{
		super(module, exported, opaque);
		this.name = name;
	}

	@Override
	public String name()
	{
		return name;
	}

	public boolean defineParam(String name, Type type)
	{
		if(paramsByName.containsKey(name))
			return false;

		Param param = new Param(type, name, params.size());
		params.add(param);
		paramsByName.put(name, param);
		return true;
	}

	private String name;
	public List<Param> params = new ArrayList<>();
	public Map<String, Param> paramsByName = new TreeMap<>();
	public Maybe<Type> ret;
}
