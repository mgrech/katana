// Copyright 2017-2018 Markus Grech
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

package io.katana.compiler.project.conditionals;

import io.katana.compiler.diag.CompileException;
import io.katana.compiler.platform.Arch;
import io.katana.compiler.platform.Os;

/*
 * condition ::= primary (junction primary)*
 * junction  ::= '&' | '|'
 * primary   ::= '(' condition ')' | ['!'] atom
 * atom      ::= arch | os
 */
public class ConditionParser
{
	public static Condition parse(String input)
	{
		ParseContext context = new ParseContext(input.replace(" ", ""));
		return parseCondition(context);
	}

	private static Condition parseCondition(ParseContext context)
	{
		Condition result = parsePrimary(context);

		while(!context.end())
		{
			int junction = context.current();
			context.advance();

			switch(junction)
			{
			case '&':
				result = new AndJunction(result, parsePrimary(context));
				break;

			case '|':
				result = new OrJunction(result, parsePrimary(context));
				break;

			default: throw new CompileException(String.format("unexpected character '%s' in input", (char)junction));
			}
		}

		return result;
	}

	private static Condition parsePrimary(ParseContext context)
	{
		if(context.current() == '(')
			return parseParens(context);

		boolean negated = false;

		if(context.current() == '!')
		{
			negated = true;
			context.advance();
		}

		Condition result = parseAtom(context);

		if(negated)
			return new NotCondition(result);

		return result;
	}

	private static Condition parseParens(ParseContext context)
	{
		context.advance();

		Condition condition = parseCondition(context);

		if(context.current() != ')')
			throw new CompileException("expected closing ')'");

		context.advance();
		return condition;
	}

	private static Condition parseAtom(ParseContext context)
	{
		StringBuilder builder = new StringBuilder();

		while(!context.end() && (Character.isLetter(context.current()) || Character.isDigit(context.current())))
		{
			builder.appendCodePoint(context.current());
			context.advance();
		}

		String keyword = builder.toString();
		String value = keyword.toUpperCase();

		try
		{
			return new ArchCondition(Arch.valueOf(value));
		}

		catch(IllegalArgumentException e)
		{}

		try
		{
			return new OsCondition(Os.valueOf(value));
		}

		catch(IllegalArgumentException e)
		{}

		throw new CompileException(String.format("unknown keyword '%s'", keyword));
	}
}
