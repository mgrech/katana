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
import katana.ast.DelayedExprParseList;
import katana.ast.expr.*;
import katana.ast.type.AstType;
import katana.diag.CompileException;
import katana.scanner.Scanner;
import katana.scanner.Token;
import katana.utils.Maybe;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ExprParser
{
	public static AstExpr parse(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		AstExpr expr = parsePrefixExpr(scanner, delayedExprs);

		if(!ParseTools.option(scanner, Token.Type.OP_INFIX, false))
			return expr;

		AstExprOpInfixList list = new AstExprOpInfixList();
		list.exprs.add(expr);

		do
		{
			String op = ParseTools.consume(scanner).value;
			list.ops.add(op);
			list.exprs.add(parsePrefixExpr(scanner, delayedExprs));
		}

		while(ParseTools.option(scanner, Token.Type.OP_INFIX, false));

		AstExprProxy proxy = new AstExprProxy(list);
		delayedExprs.infixLists.put(list, e -> proxy.expr = e);
		return proxy;
	}

	private static AstExpr parsePrefixExpr(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		if(ParseTools.option(scanner, Token.Type.OPSEQ_PREFIX, false))
		{
			String seq = ParseTools.consume(scanner).value;
			AstExprOpPrefixSeq prefixSeq = new AstExprOpPrefixSeq(seq, parsePrefixExpr(scanner, delayedExprs));
			AstExprProxy proxy = new AstExprProxy(prefixSeq);
			delayedExprs.prefixSeqs.put(prefixSeq, e -> proxy.expr = e);
			return proxy;
		}

		return parsePostfixExpr(scanner, delayedExprs);
	}

	private static AstExpr parsePostfixExpr(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		AstExpr expr = parsePrimaryExpr(scanner, delayedExprs);

		for(;;)
		{
			if(ParseTools.option(scanner, Token.Type.OPSEQ_POSTFIX, false))
			{
				String seq = ParseTools.consume(scanner).value;
				AstExprOpPostfixSeq postfixSeq = new AstExprOpPostfixSeq(expr, seq);
				AstExprProxy proxy = new AstExprProxy(postfixSeq);
				delayedExprs.postfixSeqs.put(postfixSeq, e -> proxy.expr = e);
				expr = proxy;
			}

			else if(ParseTools.option(scanner, Token.Type.PUNCT_LPAREN, true))
			{
				expr = parseFunctionCall(scanner, expr, Maybe.none(), delayedExprs);
				ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			}

			else if(ParseTools.option(scanner, Token.Type.PUNCT_LBRACKET, true))
			{
				AstExpr index = parse(scanner, delayedExprs);
				expr = new AstExprArrayAccess(expr, index);
				ParseTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
			}

			else if(ParseTools.option(scanner, ".", true))
			{
				boolean global = ParseTools.option(scanner, Token.Type.DECL_GLOBAL, true);
				String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
				expr = new AstExprMemberAccess(expr, name, global);
			}

			else
				return expr;
		}
	}

	private static AstExpr parsePrimaryExpr(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		if(ParseTools.option(scanner, Token.Type.PUNCT_LPAREN, true))
		{
			AstExpr expr = parse(scanner, delayedExprs);
			ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			return new AstExprParens(expr);
		}

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
			return parseMisc(scanner, delayedExprs);

		if(ParseTools.option(scanner, Token.Type.PUNCT_LBRACKET, true))
			return parseArrayLiteral(scanner, delayedExprs);

		if(ParseTools.option(scanner, Token.Type.TYPE_CONST, true))
			return parseConst(scanner, delayedExprs);

		if(ParseTools.option(scanner, Token.Type.PUNCT_SCOLON, true))
			throw new CompileException("';' does not denote a valid empty statement, use '{}' instead");

		ParseTools.unexpectedToken(scanner);
		throw new AssertionError("unreachable");
	}

	private static AstExpr parseConst(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		AstExpr expr = parse(scanner, delayedExprs);
		ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		return new AstExprConst(expr);
	}

	private static AstExpr parseArrayLiteral(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		Maybe<BigInteger> size = Maybe.none();

		if(!ParseTools.option(scanner, ":", true))
		{
			AstExpr sizeLit = parseLiteral(scanner);

			if(!(sizeLit instanceof AstExprLitInt))
				throw new CompileException("expected integer literal as length in array literal");

			if(((AstExprLitInt)sizeLit).type.isSome())
				throw new CompileException("length in array literal cannot have a type suffix");

			size = Maybe.some(((AstExprLitInt)sizeLit).value);
			ParseTools.expect(scanner, ":", true);
		}

		Maybe<AstType> type = Maybe.none();

		if(!ParseTools.option(scanner, ":", true))
		{
			type = Maybe.some(TypeParser.parse(scanner, delayedExprs));
			ParseTools.expect(scanner, ":", true);
		}

		List<AstExprLiteral> values = new ArrayList<>();

		if(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACKET, true))
		{
			AstExpr first = parse(scanner, delayedExprs);

			if(!(first instanceof AstExprLiteral))
				throw new CompileException("array literal elements must be literals themselves");

			values.add((AstExprLiteral)first);

			while(ParseTools.option(scanner, Token.Type.PUNCT_COMMA, true))
			{
				AstExpr next = parse(scanner, delayedExprs);

				if(!(next instanceof AstExprLiteral))
					throw new CompileException("array literal elements must be literals themselves");

				values.add((AstExprLiteral)next);
			}

			ParseTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
		}

		return new AstExprLitArray(size, type, values);
	}

	private static AstExpr parseCast(Scanner scanner, Token.Type castType, DelayedExprParseList delayedExprs)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_DOLOLOLLAR, true);
		AstType type = TypeParser.parse(scanner, delayedExprs);
		AstExpr expr = ParseTools.parenthesized(scanner, () -> parse(scanner, delayedExprs));

		switch(castType)
		{
		case MISC_WIDEN_CAST:     return new AstExprWidenCast    (type, expr);
		case MISC_NARROW_CAST:    return new AstExprNarrowCast   (type, expr);
		case MISC_SIGN_CAST:      return new AstExprSignCast     (type, expr);
		case MISC_POINTEGER_CAST: return new AstExprPointegerCast(type, expr);
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private static AstExpr parseMisc(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		Token token = ParseTools.consume(scanner);

		switch(token.type)
		{
		case MISC_SIZEOF:
			return ParseTools.parenthesized(scanner, () -> new AstExprSizeof(TypeParser.parse(scanner, delayedExprs)));

		case MISC_ALIGNOF:
			return ParseTools.parenthesized(scanner, () -> new AstExprAlignof(TypeParser.parse(scanner, delayedExprs)));

		case MISC_OFFSETOF:
			return ParseTools.parenthesized(scanner, () ->
			{
				String type = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
				ParseTools.expect(scanner, Token.Type.PUNCT_COMMA, true);
				String field = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
				return new AstExprOffsetof(type, field);
			});

		case MISC_BUILTIN:
			return parseBuiltinCall(scanner, delayedExprs);

		case MISC_INLINE:
			String inline = ParseTools.parenthesized(scanner,
				() -> ParseTools.consumeExpected(scanner, Token.Type.LIT_BOOL).value);
			String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
			ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
			AstExpr call = parseFunctionCall(scanner, new AstExprNamedSymbol(name), Maybe.some(inline.equals("true")), delayedExprs);
			ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			return call;

		case MISC_NARROW_CAST:
		case MISC_WIDEN_CAST:
		case MISC_SIGN_CAST:
		case MISC_POINTEGER_CAST:
			return parseCast(scanner, token.type, delayedExprs);

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

	private static AstExprBuiltinCall parseBuiltinCall(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		AstPath path = ParseTools.path(scanner);
		List<AstExpr> args = ParseTools.parenthesized(scanner, () -> parseArguments(scanner, delayedExprs));
		return new AstExprBuiltinCall(path.toString(), args);
	}

	private static AstExprFunctionCall parseFunctionCall(Scanner scanner, AstExpr expr, Maybe<Boolean> inline, DelayedExprParseList delayedExprs)
	{
		List<AstExpr> args = parseArguments(scanner, delayedExprs);
		return new AstExprFunctionCall(expr, args, inline);
	}

	private static List<AstExpr> parseArguments(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		if(ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
			return new ArrayList<>();

		return ParseTools.separated(scanner, Token.Type.PUNCT_COMMA, () -> parse(scanner, delayedExprs));
	}
}
