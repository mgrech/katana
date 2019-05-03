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

package io.katana.compiler.op;

public class Operator
{
	public String symbol;
	public Kind kind;
	public Associativity associativity;
	public int precedence;

	private Operator(String symbol, Kind kind, Associativity associativity, int precedence)
	{
		this.symbol = symbol;
		this.kind = kind;
		this.associativity = associativity;
		this.precedence = precedence;
	}

	public static Operator prefix(String op)
	{
		return new Operator(op, Kind.PREFIX, Associativity.NONE, 0);
	}

	public static Operator postfix(String op)
	{
		return new Operator(op, Kind.POSTFIX, Associativity.NONE, 0);
	}

	public static Operator infix(String op, Associativity associativity, int precedence)
	{
		return new Operator(op, Kind.INFIX, associativity, precedence);
	}

	public static String declName(String op, Kind kind)
	{
		return String.format("decl-%s %s", kind.toString().toLowerCase(), op);
	}

	public static String implName(String op, Kind kind)
	{
		return String.format("%s %s", kind.toString().toLowerCase(), op);
	}
}
