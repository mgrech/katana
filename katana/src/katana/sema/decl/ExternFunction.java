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

import katana.utils.Maybe;
import katana.sema.Decl;
import katana.sema.Module;
import katana.sema.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternFunction extends Decl
{
	public class Param
	{
		public Param(Type type, String name, int index)
		{
			this.type = type;
			this.name = name;
			this.index = index;
		}

		public Type type;
		public String name;
		public int index;
	}

	public ExternFunction(Module module, boolean exported, boolean opaque, String externName, String name)
	{
		super(module, exported, opaque);
		this.externName = externName;
		this.name = name;
	}

	public boolean defineParam(Type type, String name)
	{
		if(paramsByName.containsKey(name))
			return false;

		Param param = new Param(type, name, params.size());
		params.add(param);
		paramsByName.put(name, param);
		return true;
	}

	@Override
	public String name()
	{
		return name;
	}

	public String externName;
	private String name;
	public List<Param> params = new ArrayList<>();
	public Map<String, Param> paramsByName = new HashMap<>();
	public Maybe<Type> ret;
}
