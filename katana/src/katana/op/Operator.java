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

package katana.op;

public class Operator
{
	private Operator(String op, Kind kind, Associativity assoc, int prio)
	{
		this.op = op;
		this.kind = kind;
		this.assoc = assoc;
		this.prio = prio;
	}

	public static Operator prefix(String op)
	{
		return new Operator(op, Kind.PREFIX, Associativity.NONE, 0);
	}

	public static Operator postfix(String op)
	{
		return new Operator(op, Kind.POSTFIX, Associativity.NONE, 0);
	}

	public static Operator infix(String op, Associativity assoc, int prio)
	{
		return new Operator(op, Kind.INFIX, assoc, prio);
	}

	public static String declName(String op, Kind kind)
	{
		return String.format("decl-%s %s", kind.toString().toLowerCase(), op);
	}

	public static String implName(String op, Kind kind)
	{
		return String.format("%s %s", kind.toString().toLowerCase(), op);
	}

	public String op;
	public Kind kind;
	public Associativity assoc;
	public int prio;
}
