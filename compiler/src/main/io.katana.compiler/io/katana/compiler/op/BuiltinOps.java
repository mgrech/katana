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

import io.katana.compiler.ExportKind;
import io.katana.compiler.sema.decl.SemaDeclOperator;
import io.katana.compiler.utils.Maybe;

import java.util.HashMap;
import java.util.Map;

public class BuiltinOps
{
	public static final Map<String, SemaDeclOperator> PREFIX_OPS = new HashMap<>();
	public static final Map<String, SemaDeclOperator> INFIX_OPS = new HashMap<>();
	public static final Map<String, SemaDeclOperator> POSTFIX_OPS = new HashMap<>();

	private static void registerBuiltinOp(Operator op)
	{
		switch(op.kind)
		{
		case PREFIX:  PREFIX_OPS .put(op.symbol, new SemaDeclOperator(null, ExportKind.FULL, Operator.prefix(op.symbol))); break;
		case INFIX:   INFIX_OPS  .put(op.symbol, new SemaDeclOperator(null, ExportKind.FULL, Operator.infix(op.symbol, op.associativity, op.precedence))); break;
		case POSTFIX: POSTFIX_OPS.put(op.symbol, new SemaDeclOperator(null, ExportKind.FULL, Operator.postfix(op.symbol))); break;
		default: throw new AssertionError("unreachable");
		}
	}

	static
	{
		registerBuiltinOp(Operator.prefix("&"));
		registerBuiltinOp(Operator.prefix("*"));
		registerBuiltinOp(Operator.infix("=", Associativity.NONE, 0));
	}

	public static Maybe<SemaDeclOperator> find(String symbol, Kind kind)
	{
		switch(kind)
		{
		case PREFIX:  return Maybe.wrap(PREFIX_OPS .get(symbol));
		case INFIX:   return Maybe.wrap(INFIX_OPS  .get(symbol));
		case POSTFIX: return Maybe.wrap(POSTFIX_OPS.get(symbol));
		default: break;
		}

		throw new AssertionError("unreachable");
	}
}
