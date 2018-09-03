// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.sema.decl;

import io.katana.compiler.ExportKind;
import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.SemaSymbol;
import io.katana.compiler.sema.scope.SemaScopeFunction;
import io.katana.compiler.sema.type.SemaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class SemaDeclFunction extends SemaDecl
{
	public class Param implements SemaSymbol
	{
		public SemaType type;
		public String name;
		public int index;

		public Param(SemaType type, String name, int index)
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
	}

	private String name;
	public List<Param> params = new ArrayList<>();
	public Map<String, Param> paramsByName = new TreeMap<>();
	public SemaType returnType;
	public SemaScopeFunction scope;

	protected SemaDeclFunction(SemaModule module, ExportKind exportKind, String name)
	{
		super(module, exportKind);
		this.name = name;
	}

	@Override
	public String name()
	{
		return name;
	}

	public boolean defineParam(String name, SemaType type)
	{
		if(paramsByName.containsKey(name))
			return false;

		var param = new Param(type, name, params.size());
		params.add(param);
		paramsByName.put(name, param);
		return true;
	}
}
