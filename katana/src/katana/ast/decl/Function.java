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

package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Stmt;
import katana.ast.Type;
import katana.utils.Maybe;

import java.util.ArrayList;

public class Function extends Decl
{
	public static class Param
	{
		public Param(Type type, String name)
		{
			this.type = type;
			this.name = name;
		}

		public Type type;
		public String name;
	}

	public static class Local
	{
		public Local(Type type, String name)
		{
			this.type = type;
			this.name = name;
		}

		public Type type;
		public String name;
	}

	public Function(boolean exported, boolean opaque, String name, ArrayList<Param> params, Maybe<Type> ret, ArrayList<Local> locals, ArrayList<Stmt> body)
	{
		super(exported, opaque);
		this.name = name;
		this.params = params;
		this.ret = ret;
		this.locals = locals;
		this.body = body;
	}

	public String name;
	public ArrayList<Param> params;
	public Maybe<Type> ret;
	public ArrayList<Local> locals;
	public ArrayList<Stmt> body;

	@Override
	public String toString()
	{
		StringBuilder params = new StringBuilder();

		for(Param param : this.params)
		{
			params.append("\t\t");
			params.append(param.name);
			params.append(" (");
			params.append(param.type);
			params.append(")\n");
		}

		String ret = this.ret.isSome() ? "\tret: " + this.ret.unwrap() + '\n' : "";

		StringBuilder locals = new StringBuilder();

		for(Local local : this.locals)
		{
			locals.append("\t\t");
			locals.append(local.name);
			locals.append(" (");
			locals.append(local.type);
			locals.append(")\n");
		}

		StringBuilder body = new StringBuilder();

		for(Stmt stmt : this.body)
		{
			body.append("\t\t");
			body.append(stmt);
			body.append('\n');
		}

		return String.format("%sname: %s\n\tparams:\n%s%s\tlocals:\n%s\tbody:\n%s", super.toString(), name, params, ret, locals, body);
	}
}
