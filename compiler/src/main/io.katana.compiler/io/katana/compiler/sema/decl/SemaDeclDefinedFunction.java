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
import io.katana.compiler.sema.stmt.SemaStmt;
import io.katana.compiler.sema.stmt.SemaStmtLabel;
import io.katana.compiler.sema.type.SemaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SemaDeclDefinedFunction extends SemaDeclFunction
{
	public class Variable implements SemaSymbol
	{
		public Variable(String name, SemaType type, int index)
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

	public List<Variable> variables = new ArrayList<>();
	public Map<String, Variable> variablesByName = new TreeMap<>();
	public List<SemaStmt> body = new ArrayList<>();
	public Map<String, SemaStmtLabel> labels = new TreeMap<>();

	public SemaDeclDefinedFunction(SemaModule module, ExportKind exportKind, String name)
	{
		super(module, exportKind, name);
	}

	public boolean defineVariable(String name, SemaType type)
	{
		if(variablesByName.containsKey(name))
			return false;

		var var_ = new Variable(name, type, variables.size());
		variables.add(var_);
		variablesByName.put(name, var_);
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
}
