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

package katana.parser;

import katana.utils.Maybe;
import katana.ast.Expr;
import katana.ast.Path;
import katana.ast.Type;
import katana.ast.expr.*;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

public class ExprParser
{
	public static Expr parse(Scanner scanner)
	{
		Expr expr = parseInitialExpr(scanner);

		for(;;)
		{
			Token token = scanner.token();

			switch(token.type)
			{
			case PUNCT_LPAREN:
				scanner.advance();
				expr = parseFunctionCall(scanner, expr, Maybe.none());
				ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
				break;

			case PUNCT_LBRACKET:
				scanner.advance();
				Expr index = ExprParser.parse(scanner);
				expr = new ArrayAccess(expr, index);
				ParseTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
				break;

			case PUNCT_DOT:
				scanner.advance();
				String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
				expr = new MemberAccess(expr, name);
				break;

			case PUNCT_ASSIGN:
				scanner.advance();
				Expr value = parse(scanner);
				expr = new Assign(expr, value);
				break;

			default: return expr;
			}
		}
	}

	private static Expr parseInitialExpr(Scanner scanner)
	{
		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParseTools.consume(scanner).value;
			return new NamedValue(name);
		}

		if(ParseTools.option(scanner, Token.Category.LIT, false))
			return parseLiteral(scanner);

		if(ParseTools.option(scanner, Token.Category.MISC, false))
			return parseMisc(scanner);

		ParseTools.unexpectedToken(scanner);
		throw new AssertionError("unreachable");
	}

	private static Expr parseMisc(Scanner scanner)
	{
		Token token = ParseTools.consume(scanner);

		switch(token.type)
		{
		case MISC_SIZEOF:
			Type stype = ParseTools.parenthesized(scanner, () -> TypeParser.parse(scanner));
			return new Sizeof(stype);

		case MISC_ALIGNOF:
			Type atype = ParseTools.parenthesized(scanner, () -> TypeParser.parse(scanner));
			return new Alignof(atype);

		case MISC_OFFSETOF:
			return ParseTools.parenthesized(scanner, () ->
			{
				String type = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
				ParseTools.expect(scanner, Token.Type.PUNCT_COMMA, true);
				String field = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
				return new Offsetof(type, field);
			});

		case MISC_BUILTIN:
			return parseBuiltinCall(scanner);

		case MISC_INLINE:
			String inline = ParseTools.parenthesized(scanner,
				() -> ParseTools.consumeExpected(scanner, Token.Type.LIT_BOOL).value);
			String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
			ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
			Expr call = parseFunctionCall(scanner, new NamedValue(name), Maybe.some(inline.equals("true")));
			ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			return call;

		case MISC_ADDRESSOF:
			Expr aexpr = ParseTools.parenthesized(scanner, () -> ExprParser.parse(scanner));
			return new Addressof(aexpr);

		case MISC_DEREF:
			return ParseTools.parenthesized(scanner, () ->
			{
				Type dtype = TypeParser.parse(scanner);
				ParseTools.expect(scanner, Token.Type.PUNCT_COMMA, true);
				Expr dexpr = ExprParser.parse(scanner);
				return new Deref(dtype, dexpr);
			});

		default: throw new AssertionError("unreachable");
		}
	}

	private static Expr parseLiteral(Scanner scanner)
	{
		Token token = ParseTools.consume(scanner);

		switch(token.type)
		{
		case LIT_NULL:   return new LitNull();
		case LIT_BOOL:   return new LitBool(token.value.equals("true"));
		case LIT_INT:    return new LitInt(new BigInteger(token.value));
		case LIT_FLOAT:  return new LitFloat(new BigDecimal(token.value));
		case LIT_STRING: return new LitString(token.value);

		default: throw new AssertionError("unreachable");
		}
	}

	private static BuiltinCall parseBuiltinCall(Scanner scanner)
	{
		Path path = ParseTools.path(scanner);
		ArrayList<Expr> args = ParseTools.parenthesized(scanner, () -> parseArguments(scanner));
		return new BuiltinCall(path.toString(), args);
	}

	private static FunctionCall parseFunctionCall(Scanner scanner, Expr expr, Maybe<Boolean> inline)
	{
		ArrayList<Expr> args = parseArguments(scanner);
		return new FunctionCall(expr, args, inline);
	}

	private static ArrayList<Expr> parseArguments(Scanner scanner)
	{
		if(ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
			return new ArrayList<>();

		return ParseTools.separated(scanner, Token.Type.PUNCT_COMMA, () -> parse(scanner));
	}
}
