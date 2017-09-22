// Copyright 2016-2017 Markus Grech
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

package katana.backend.llvm;

import katana.op.Kind;
import katana.sema.decl.SemaDeclDefinedOperator;
import katana.sema.decl.SemaDeclFunction;

import java.util.Map;
import java.util.TreeMap;

public class FunctionNameMangler
{
	private static final Map<Character, String> OP_MANGLING = new TreeMap<>();

	static
	{
		OP_MANGLING.put('.', "dot");
		OP_MANGLING.put(':', "colon");
		OP_MANGLING.put('?', "questionmark");
		OP_MANGLING.put('!', "exclamationmark");
		OP_MANGLING.put('=', "equals");
		OP_MANGLING.put('<', "lessthan");
		OP_MANGLING.put('>', "greaterthan");
		OP_MANGLING.put('+', "plus");
		OP_MANGLING.put('-', "minus");
		OP_MANGLING.put('*', "asterisk");
		OP_MANGLING.put('/', "slash");
		OP_MANGLING.put('%', "percent");
		OP_MANGLING.put('&', "ampersand");
		OP_MANGLING.put('|', "pipe");
		OP_MANGLING.put('^', "caret");
		OP_MANGLING.put('~', "tilde");
		OP_MANGLING.put('$', "dollar");
	}

	private static String mangleOperatorSymbol(String op)
	{
		StringBuilder builder = new StringBuilder();

		for(char c : op.toCharArray())
		{
			String mangled = OP_MANGLING.get(c);

			if(mangled == null)
				throw new AssertionError("unreachable");

			builder.append(mangled);
		}

		return builder.toString();
	}

	private static String mangleOperatorName(SemaDeclDefinedOperator operator)
	{
		String op = operator.decl.operator.symbol;
		Kind kind = operator.decl.operator.kind;
		String path = operator.decl.module().name();
		return String.format("op-%s-%s.%s", kind.toString().toLowerCase(), path, mangleOperatorSymbol(op));
	}

	private static String mangleFunctionName(SemaDeclFunction function)
	{
		if(function instanceof SemaDeclDefinedOperator)
			return function.module().name() + "." + mangleOperatorName((SemaDeclDefinedOperator)function);

		return function.qualifiedName().toString();
	}

	public static String mangle(SemaDeclFunction function)
	{
		StringBuilder builder = new StringBuilder();

		for(SemaDeclFunction.Param param : function.params)
		{
			builder.append('$');
			builder.append(TypeMangler.mangle(param.type));
		}

		return mangleFunctionName(function) + builder.toString();
	}
}
