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

import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.SemaSymbol;
import io.katana.compiler.sema.stmt.SemaStmt;
import io.katana.compiler.sema.stmt.SemaStmtLabel;
import io.katana.compiler.sema.type.SemaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SemaDeclDefinedFunction extends SemaDeclFunction
{
	public class Local implements SemaSymbol
	{
		public Local(String name, SemaType type, int index)
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
		public SemaType type;
		public int index;
	}

	public SemaDeclDefinedFunction(SemaModule module, boolean exported, boolean opaque, String name)
	{
		super(module, exported, opaque, name);
	}

	public boolean defineLocal(String name, SemaType type)
	{
		if(localsByName.containsKey(name))
			return false;

		Local local = new Local(name, type, locals.size());
		locals.add(local);
		localsByName.put(name, local);
		return true;
	}

	public boolean defineLabel(SemaStmtLabel label)
	{
		if(labels.containsKey(label.name))
			return false;

		labels.put(label.name, label);
		return true;
	}

	public void add(SemaStmt stmt)
	{
		body.add(stmt);
	}

	public List<Local> locals = new ArrayList<>();
	public Map<String, Local> localsByName = new TreeMap<>();
	public List<SemaStmt> body = new ArrayList<>();
	public Map<String, SemaStmtLabel> labels = new TreeMap<>();
}
