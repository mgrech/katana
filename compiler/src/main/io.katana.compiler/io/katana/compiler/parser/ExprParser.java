// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.parser;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.ast.AstPath;
import io.katana.compiler.ast.expr.*;
import io.katana.compiler.ast.type.AstType;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.scanner.Token;
import io.katana.compiler.scanner.TokenCategory;
import io.katana.compiler.scanner.TokenType;
import io.katana.compiler.utils.Maybe;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ExprParser
{
	public static AstExpr parse(ParseContext ctx)
	{
		AstExpr expr = parsePrefixExpr(ctx);

		if(!ParseTools.option(ctx, TokenType.OP_INFIX, false))
			return expr;

		AstExprOpInfixList list = new AstExprOpInfixList();
		list.exprs.add(expr);

		do
		{
			var op = (String)ParseTools.consume(ctx).value;
			list.ops.add(op);
			list.exprs.add(parsePrefixExpr(ctx));
		}
		while(ParseTools.option(ctx, TokenType.OP_INFIX, false));

		AstExprProxy proxy = new AstExprProxy(list);
		ctx.lateParseExprs().infixLists.put(list, e -> proxy.expr = e);
		return proxy;
	}

	private static AstExpr parsePrefixExpr(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.OP_PREFIX_SEQ, false))
		{
			var seq = (String)ParseTools.consume(ctx).value;
			AstExprOpPrefixSeq prefixSeq = new AstExprOpPrefixSeq(seq, parsePrefixExpr(ctx));
			AstExprProxy proxy = new AstExprProxy(prefixSeq);
			ctx.lateParseExprs().prefixSeqs.put(prefixSeq, e -> proxy.expr = e);
			return proxy;
		}

		return parsePostfixExpr(ctx);
	}

	private static AstExpr parsePostfixExpr(ParseContext ctx)
	{
		AstExpr expr = parsePrimaryExpr(ctx);

		for(;;)
		{
			if(ParseTools.option(ctx, TokenType.OP_POSTFIX_SEQ, false))
			{
				var seq = (String)ParseTools.consume(ctx).value;
				AstExprOpPostfixSeq postfixSeq = new AstExprOpPostfixSeq(expr, seq);
				AstExprProxy proxy = new AstExprProxy(postfixSeq);
				ctx.lateParseExprs().postfixSeqs.put(postfixSeq, e -> proxy.expr = e);
				expr = proxy;
			}

			else if(ParseTools.option(ctx, TokenType.PUNCT_LPAREN, true))
			{
				expr = parseFunctionCall(ctx, expr, Maybe.none());
				ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
			}

			else if(ParseTools.option(ctx, TokenType.PUNCT_LBRACKET, true))
			{
				AstExpr index = parse(ctx);
				expr = new AstExprArrayAccess(expr, index);
				ParseTools.expect(ctx, TokenType.PUNCT_RBRACKET, true);
			}

			else if(ParseTools.option(ctx, ".", true))
			{
				boolean global = ParseTools.option(ctx, TokenType.DECL_GLOBAL, true);
				String name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
				expr = new AstExprMemberAccess(expr, name, global);
			}

			else
				return expr;
		}
	}

	private static AstExpr parsePrimaryExpr(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.PUNCT_LPAREN, true))
		{
			AstExpr expr = parse(ctx);
			ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
			return new AstExprParens(expr);
		}

		if(ParseTools.option(ctx, TokenType.DECL_GLOBAL, true))
		{
			var name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
			return new AstExprNamedGlobal(name);
		}

		if(ParseTools.option(ctx, TokenType.IDENT, false))
		{
			var name = (String)ParseTools.consume(ctx).value;
			return new AstExprNamedSymbol(name);
		}

		if(ParseTools.option(ctx, TokenCategory.LIT, false))
			return parseLiteral(ctx);

		if(ParseTools.option(ctx, TokenCategory.MISC, false))
			return parseMisc(ctx);

		if(ParseTools.option(ctx, TokenType.PUNCT_LBRACKET, true))
			return parseArrayLiteral(ctx);

		if(ParseTools.option(ctx, TokenType.TYPE_CONST, true))
			return parseConst(ctx);

		if(ParseTools.option(ctx, TokenType.PUNCT_SCOLON, false))
		{
			ctx.error(ParserDiagnostics.SEMICOLON_IS_INVALID_EMPTY_STATEMENT);
			ctx.advance();
			throw new CompileException(ctx.diagnostics().summary());
		}

		if(ctx.token() == null)
			throw new CompileException("unexpected end of file");

		ParseTools.unexpectedToken(ctx);
		throw new AssertionError("unreachable");
	}

	private static AstExpr parseConst(ParseContext ctx)
	{
		ParseTools.expect(ctx, TokenType.PUNCT_LPAREN, true);
		AstExpr expr = parse(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
		return new AstExprConst(expr);
	}

	private static AstExpr parseArrayLiteral(ParseContext ctx)
	{
		Maybe<BigInteger> size = Maybe.none();

		if(!ParseTools.option(ctx, ":", true))
		{
			AstExpr sizeLit = parseLiteral(ctx);

			if(!(sizeLit instanceof AstExprLitInt))
				throw new CompileException("expected integer literal as length in array literal");

			if(((AstExprLitInt)sizeLit).type.isSome())
				throw new CompileException("length in array literal cannot have a type suffix");

			size = Maybe.some(((AstExprLitInt)sizeLit).value);
			ParseTools.expect(ctx, ":", true);
		}

		Maybe<AstType> type = Maybe.none();

		if(!ParseTools.option(ctx, ":", true))
		{
			type = Maybe.some(TypeParser.parse(ctx));
			ParseTools.expect(ctx, ":", true);
		}

		List<AstExprLiteral> values = new ArrayList<>();

		if(!ParseTools.option(ctx, TokenType.PUNCT_RBRACKET, true))
		{
			AstExpr first = parse(ctx);

			if(!(first instanceof AstExprLiteral))
				throw new CompileException("array literal elements must be literals themselves");

			values.add((AstExprLiteral)first);

			while(ParseTools.option(ctx, TokenType.PUNCT_COMMA, true))
			{
				AstExpr next = parse(ctx);

				if(!(next instanceof AstExprLiteral))
					throw new CompileException("array literal elements must be literals themselves");

				values.add((AstExprLiteral)next);
			}

			ParseTools.expect(ctx, TokenType.PUNCT_RBRACKET, true);
		}

		return new AstExprLitArray(size, type, values);
	}

	private static AstExpr parseCast(ParseContext ctx, TokenType castType)
	{
		ParseTools.expect(ctx, TokenType.PUNCT_DOLLAR, true);
		AstType type = TypeParser.parse(ctx);
		AstExpr expr = ParseTools.parenthesized(ctx, () -> parse(ctx));

		switch(castType)
		{
		case MISC_WIDEN_CAST:   return new AstExprWidenCast    (type, expr);
		case MISC_NARROW_CAST:  return new AstExprNarrowCast   (type, expr);
		case MISC_SIGN_CAST:    return new AstExprSignCast     (type, expr);
		case MISC_POINTER_CAST: return new AstExprPointerCast(type, expr);
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private static AstExpr parseMisc(ParseContext ctx)
	{
		Token token = ParseTools.consume(ctx);

		switch(token.type)
		{
		case MISC_SIZEOF:
			return ParseTools.parenthesized(ctx, () -> new AstExprSizeof(TypeParser.parse(ctx)));

		case MISC_ALIGNOF:
			return ParseTools.parenthesized(ctx, () -> new AstExprAlignof(TypeParser.parse(ctx)));

		case MISC_OFFSETOF:
			return ParseTools.parenthesized(ctx, () ->
			{
				var type = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
				ParseTools.expect(ctx, TokenType.PUNCT_COMMA, true);
				var field = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
				return new AstExprOffsetof(type, field);
			});

		case MISC_BUILTIN:
			return parseBuiltinCall(ctx);

		case MISC_INLINE:
			var inline = ParseTools.parenthesized(ctx,
				() -> (boolean)ParseTools.consumeExpected(ctx, TokenType.LIT_BOOL).value);
			var name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
			ParseTools.expect(ctx, TokenType.PUNCT_LPAREN, true);
			var call = parseFunctionCall(ctx, new AstExprNamedSymbol(name), Maybe.some(inline));
			ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
			return call;

		case MISC_NARROW_CAST:
		case MISC_WIDEN_CAST:
		case MISC_SIGN_CAST:
		case MISC_POINTER_CAST:
			return parseCast(ctx, token.type);

		default: throw new AssertionError("unreachable");
		}
	}

	private static AstExpr parseLiteral(ParseContext ctx)
	{
		Token token = ParseTools.consume(ctx);

		switch(token.type)
		{
		case LIT_NULL: return new AstExprLitNull();
		case LIT_BOOL: return new AstExprLitBool((boolean)token.value);

		case LIT_INT:   return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT));
		case LIT_INT8:  return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT8));
		case LIT_INT16: return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT16));
		case LIT_INT32: return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT32));
		case LIT_INT64: return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT64));

		case LIT_UINT:   return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT));
		case LIT_UINT8:  return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT8));
		case LIT_UINT16: return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT16));
		case LIT_UINT32: return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT32));
		case LIT_UINT64: return new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT64));

		case LIT_FLOAT32: return new AstExprLitFloat((BigDecimal)token.value, Maybe.some(BuiltinType.FLOAT32));
		case LIT_FLOAT64: return new AstExprLitFloat((BigDecimal)token.value, Maybe.some(BuiltinType.FLOAT64));

		case LIT_STRING: return new AstExprLitString((String)token.value);

		case LIT_INT_DEDUCE:   return new AstExprLitInt((BigInteger)token.value, Maybe.none());
		case LIT_FLOAT_DEDUCE: return new AstExprLitFloat((BigDecimal)token.value, Maybe.none());

		default: throw new AssertionError("unreachable");
		}
	}

	private static AstExprBuiltinCall parseBuiltinCall(ParseContext ctx)
	{
		AstPath path = ParseTools.path(ctx);
		List<AstExpr> args = ParseTools.parenthesized(ctx, () -> parseArguments(ctx));
		return new AstExprBuiltinCall(path.toString(), args);
	}

	private static AstExprFunctionCall parseFunctionCall(ParseContext ctx, AstExpr expr, Maybe<Boolean> inline)
	{
		List<AstExpr> args = parseArguments(ctx);
		return new AstExprFunctionCall(expr, args, inline);
	}

	private static List<AstExpr> parseArguments(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.PUNCT_RPAREN, false))
			return new ArrayList<>();

		return ParseTools.separated(ctx, TokenType.PUNCT_COMMA, () -> parse(ctx));
	}
}
