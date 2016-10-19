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

import katana.BuiltinType;
import katana.ast.AstPath;
import katana.ast.expr.*;
import katana.ast.type.AstType;
import katana.scanner.Scanner;
import katana.scanner.Token;
import katana.utils.Maybe;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ExprParser
{
	public static AstExpr parse(Scanner scanner)
	{
		AstExpr expr = parsePrimaryExpr(scanner);

		for(;;)
		{
			Token token = scanner.state().token;

			switch(token.type)
			{
			case PUNCT_LPAREN:
				scanner.advance();
				expr = parseFunctionCall(scanner, expr, Maybe.none());
				ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
				break;

			case PUNCT_LBRACKET:
				scanner.advance();
				AstExpr index = ExprParser.parse(scanner);
				expr = new AstExprArrayAccess(expr, index);
				ParseTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
				break;

			case PUNCT_DOT:
				scanner.advance();
				boolean global = ParseTools.option(scanner, Token.Type.DECL_GLOBAL, true);
				String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
				expr = new AstExprMemberAccess(expr, name, global);
				break;

			case PUNCT_ASSIGN:
				scanner.advance();
				AstExpr value = parse(scanner);
				expr = new AstExprAssign(expr, value);
				break;

			default: return expr;
			}
		}
	}

	private static AstExpr parsePrimaryExpr(Scanner scanner)
	{
		if(ParseTools.option(scanner, Token.Type.DECL_GLOBAL, true))
		{
			String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
			return new AstExprNamedGlobal(name);
		}

		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParseTools.consume(scanner).value;
			return new AstExprNamedSymbol(name);
		}

		if(ParseTools.option(scanner, Token.Category.LIT, false))
			return parseLiteral(scanner);

		if(ParseTools.option(scanner, Token.Category.MISC, false))
			return parseMisc(scanner);

		if(ParseTools.option(scanner, Token.Type.PUNCT_LBRACKET, true))
			return parseArrayLiteral(scanner);

		if(ParseTools.option(scanner, Token.Type.TYPE_CONST, true))
			return parseConst(scanner);

		ParseTools.unexpectedToken(scanner);
		throw new AssertionError("unreachable");
	}

	private static AstExpr parseConst(Scanner scanner)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		AstExpr expr = parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		return new AstExprConst(expr);
	}

	private static AstExpr parseArrayLiteral(Scanner scanner)
	{
		Maybe<BigInteger> size = Maybe.none();

		if(!ParseTools.option(scanner, Token.Type.PUNCT_COLON, true))
		{
			AstExpr sizeLit = parseLiteral(scanner);

			if(!(sizeLit instanceof AstExprLitInt))
				throw new RuntimeException("expected integer literal as length in array literal");

			if(((AstExprLitInt)sizeLit).type.isSome())
				throw new RuntimeException("length in array literal cannot have a type suffix");

			size = Maybe.some(((AstExprLitInt)sizeLit).value);
			ParseTools.expect(scanner, Token.Type.PUNCT_COLON, true);
		}

		Maybe<AstType> type = Maybe.none();

		if(!ParseTools.option(scanner, Token.Type.PUNCT_COLON, true))
		{
			type = Maybe.some(TypeParser.parse(scanner));
			ParseTools.expect(scanner, Token.Type.PUNCT_COLON, true);
		}

		List<AstExprLiteral> values = new ArrayList<>();

		if(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACKET, true))
		{
			AstExpr first = parse(scanner);

			if(!(first instanceof AstExprLiteral))
				throw new RuntimeException("array literal elements must be literals themselves");

			values.add((AstExprLiteral)first);

			while(ParseTools.option(scanner, Token.Type.PUNCT_COMMA, true))
			{
				AstExpr next = parse(scanner);

				if(!(next instanceof AstExprLiteral))
					throw new RuntimeException("array literal elements must be literals themselves");

				values.add((AstExprLiteral)next);
			}

			ParseTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
		}

		return new AstExprLitArray(size, type, values);
	}

	private static AstExpr parseMisc(Scanner scanner)
	{
		Token token = ParseTools.consume(scanner);

		switch(token.type)
		{
		case MISC_SIZEOF:
			AstType stype = ParseTools.parenthesized(scanner, () -> TypeParser.parse(scanner));
			return new AstExprSizeof(stype);

		case MISC_ALIGNOF:
			AstType atype = ParseTools.parenthesized(scanner, () -> TypeParser.parse(scanner));
			return new AstExprAlignof(atype);

		case MISC_OFFSETOF:
			return ParseTools.parenthesized(scanner, () ->
			{
				String type = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
				ParseTools.expect(scanner, Token.Type.PUNCT_COMMA, true);
				String field = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
				return new AstExprOffsetof(type, field);
			});

		case MISC_BUILTIN:
			return parseBuiltinCall(scanner);

		case MISC_INLINE:
			String inline = ParseTools.parenthesized(scanner,
				() -> ParseTools.consumeExpected(scanner, Token.Type.LIT_BOOL).value);
			String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
			ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
			AstExpr call = parseFunctionCall(scanner, new AstExprNamedSymbol(name), Maybe.some(inline.equals("true")));
			ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			return call;

		case MISC_ADDRESSOF:
			AstExpr aexpr = ParseTools.parenthesized(scanner, () -> ExprParser.parse(scanner));
			return new AstExprAddressof(aexpr);

		case MISC_DEREF:
			return ParseTools.parenthesized(scanner, () ->
			{
				AstExpr dexpr = ExprParser.parse(scanner);
				return new AstExprDeref(dexpr);
			});

		default: throw new AssertionError("unreachable");
		}
	}

	private static AstExpr parseLiteral(Scanner scanner)
	{
		Token token = ParseTools.consume(scanner);

		switch(token.type)
		{
		case LIT_NULL: return new AstExprLitNull();
		case LIT_BOOL: return new AstExprLitBool((boolean)token.data);

		case LIT_INT:   return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.INT));
		case LIT_PINT:  return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.PINT));
		case LIT_INT8:  return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.INT8));
		case LIT_INT16: return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.INT16));
		case LIT_INT32: return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.INT32));
		case LIT_INT64: return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.INT64));

		case LIT_UINT:   return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.UINT));
		case LIT_UPINT:  return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.UPINT));
		case LIT_UINT8:  return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.UINT8));
		case LIT_UINT16: return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.UINT16));
		case LIT_UINT32: return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.UINT32));
		case LIT_UINT64: return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.some(BuiltinType.UINT64));

		case LIT_FLOAT32: return new AstExprLitFloat(new BigDecimal(token.value), Maybe.some(BuiltinType.FLOAT32));
		case LIT_FLOAT64: return new AstExprLitFloat(new BigDecimal(token.value), Maybe.some(BuiltinType.FLOAT64));

		case LIT_STRING: return new AstExprLitString(token.value);

		case LIT_INT_DEDUCE:   return new AstExprLitInt(new BigInteger(token.value, (int)token.data), Maybe.none());
		case LIT_FLOAT_DEDUCE: return new AstExprLitFloat(new BigDecimal(token.value), Maybe.none());

		default: throw new AssertionError("unreachable");
		}
	}

	private static AstExprBuiltinCall parseBuiltinCall(Scanner scanner)
	{
		AstPath path = ParseTools.path(scanner);
		List<AstExpr> args = ParseTools.parenthesized(scanner, () -> parseArguments(scanner));
		return new AstExprBuiltinCall(path.toString(), args);
	}

	private static AstExprFunctionCall parseFunctionCall(Scanner scanner, AstExpr expr, Maybe<Boolean> inline)
	{
		List<AstExpr> args = parseArguments(scanner);
		return new AstExprFunctionCall(expr, args, inline);
	}

	private static List<AstExpr> parseArguments(Scanner scanner)
	{
		if(ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
			return new ArrayList<>();

		return ParseTools.separated(scanner, Token.Type.PUNCT_COMMA, () -> parse(scanner));
	}
}
