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
import katana.scanner.Token;
import katana.scanner.TokenCategory;
import katana.scanner.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ParseTools
{
	public static AstPath path(ParseContext ctx)
	{
		Supplier<String> parseComponent = () -> consumeExpected(ctx, TokenType.IDENT).value;
		List<String> components = separated(ctx, ".", parseComponent);
		return new AstPath(components);
	}

	public static <T> List<T> separated(ParseContext ctx, String separator, Supplier<T> parser)
	{
		List<T> result = new ArrayList<>();

		do result.add(parser.get());
		while(option(ctx, separator, true));

		return result;
	}

	public static <T> List<T> separated(ParseContext ctx, TokenType separator, Supplier<T> parser)
	{
		List<T> result = new ArrayList<>();

		do result.add(parser.get());
		while(option(ctx, separator, true));

		return result;
	}

	public static <T> T parenthesized(ParseContext ctx, Supplier<T> func)
	{
		ParseTools.expect(ctx, TokenType.PUNCT_LPAREN, true);
		T result = func.get();
		ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
		return result;
	}

	public static boolean option(ParseContext ctx, Predicate<Token> predicate, boolean eat)
	{
		if(!predicate.test(ctx.token()))
			return false;

		if(eat)
			ctx.advance();

		return true;
	}

	public static boolean option(ParseContext ctx, String value, boolean eat)
	{
		return option(ctx, t -> t.value.equals(value), eat);
	}

	public static boolean option(ParseContext ctx, TokenCategory category, boolean eat)
	{
		return option(ctx, t -> t.category == category, eat);
	}

	public static boolean option(ParseContext ctx, TokenType type, boolean eat)
	{
		return option(ctx, t -> t.type == type, eat);
	}

	public static void expect(ParseContext ctx, String value, boolean eat)
	{
		if(!option(ctx, value, eat))
			unexpectedToken(ctx, value);
	}

	public static void expect(ParseContext ctx, TokenCategory category, boolean eat)
	{
		if(!option(ctx, category, eat))
			unexpectedToken(ctx, category);
	}

	public static void expect(ParseContext ctx, TokenType type, boolean eat)
	{
		if(!option(ctx, type, eat))
			unexpectedToken(ctx, type);
	}

	public static Token consume(ParseContext ctx)
	{
		Token token = ctx.token();
		ctx.advance();
		return token;
	}

	public static Token consumeExpected(ParseContext ctx, TokenCategory category)
	{
		expect(ctx, category, false);
		return consume(ctx);
	}

	public static Token consumeExpected(ParseContext ctx, TokenType type)
	{
		expect(ctx, type, false);
		return consume(ctx);
	}

	public static <T> void unexpectedToken(ParseContext ctx, T expected)
	{
		ctx.error(null, "unexpected token '%s', expected '%s'", ctx.token().value, expected);
	}

	public static void unexpectedToken(ParseContext ctx)
	{
		ctx.error(null, "unexpected token '%s'", ctx.token().value);
	}
}
