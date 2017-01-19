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

package katana.parser;

import katana.ast.AstPath;
import katana.diag.CompileException;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ParseTools
{
	public static AstPath path(Scanner scanner)
	{
		Supplier<String> parseComponent = () -> consumeExpected(scanner, Token.Type.IDENT).value;
		List<String> components = separated(scanner, ".", parseComponent);
		return new AstPath(components);
	}

	public static <T> List<T> separated(Scanner scanner, String separator, Supplier<T> parser)
	{
		List<T> result = new ArrayList<>();

		do result.add(parser.get());
		while(option(scanner, separator, true));

		return result;
	}

	public static <T> List<T> separated(Scanner scanner, Token.Type separator, Supplier<T> parser)
	{
		List<T> result = new ArrayList<>();

		do result.add(parser.get());
		while(option(scanner, separator, true));

		return result;
	}

	public static <T> T parenthesized(Scanner scanner, Supplier<T> func)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		T result = func.get();
		ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		return result;
	}

	public static boolean option(Scanner scanner, Predicate<Token> predicate, boolean eat)
	{
		if(!predicate.test(scanner.state().token))
			return false;

		if(eat)
			scanner.advance();

		return true;
	}

	public static boolean option(Scanner scanner, String value, boolean eat)
	{
		return option(scanner, t -> t.value.equals(value), eat);
	}

	public static boolean option(Scanner scanner, Token.Category category, boolean eat)
	{
		return option(scanner, t -> t.category == category, eat);
	}

	public static boolean option(Scanner scanner, Token.Type type, boolean eat)
	{
		return option(scanner, t -> t.type == type, eat);
	}

	public static void expect(Scanner scanner, String value, boolean eat)
	{
		if(!option(scanner, value, eat))
			unexpectedToken(scanner, value);
	}

	public static void expect(Scanner scanner, Token.Category category, boolean eat)
	{
		if(!option(scanner, category, eat))
			unexpectedToken(scanner, category);
	}

	public static void expect(Scanner scanner, Token.Type type, boolean eat)
	{
		if(!option(scanner, type, eat))
			unexpectedToken(scanner, type);
	}

	public static Token consume(Scanner scanner)
	{
		Token token = scanner.state().token;
		scanner.advance();
		return token;
	}

	public static Token consumeExpected(Scanner scanner, Token.Category category)
	{
		expect(scanner, category, false);
		return consume(scanner);
	}

	public static Token consumeExpected(Scanner scanner, Token.Type type)
	{
		expect(scanner, type, false);
		return consume(scanner);
}

	public static <T> void unexpectedToken(Scanner scanner, T expected)
	{
		String fmt = "unexpected token '%s', expected '%s' on line %s, column %s";
		throw new CompileException(String.format(fmt, scanner.state().token.value, expected, scanner.state().line, scanner.state().tokenColumn));
	}

	public static void unexpectedToken(Scanner scanner)
	{
		String fmt = "unexpected token '%s' on line %s, column %s";
		throw new CompileException(String.format(fmt, scanner.state().token.value, scanner.state().line, scanner.state().tokenColumn));
	}
}
