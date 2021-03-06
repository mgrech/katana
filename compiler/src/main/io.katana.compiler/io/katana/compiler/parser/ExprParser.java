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

package io.katana.compiler.parser;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.Inlining;
import io.katana.compiler.ast.expr.*;
import io.katana.compiler.ast.type.AstType;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.scanner.TokenCategory;
import io.katana.compiler.scanner.TokenType;
import io.katana.compiler.utils.Fraction;
import io.katana.compiler.utils.Maybe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ExprParser
{
	public static AstExpr parse(ParseContext ctx)
	{
		var expr = parsePrefixExpr(ctx);

		if(!ParseTools.option(ctx, TokenType.OP_INFIX, false))
			return expr;

		var list = new AstExprOpInfixList();
		list.nestedExprs.add(expr);

		do
		{
			var op = (String)ParseTools.consume(ctx).value;
			list.infixOps.add(op);
			list.nestedExprs.add(parsePrefixExpr(ctx));
		}
		while(ParseTools.option(ctx, TokenType.OP_INFIX, false));

		var proxy = new AstExprProxy(list);
		ctx.lateParseExprs().infixLists.put(list, e -> proxy.nestedExpr = e);
		return proxy;
	}

	private static AstExpr parsePrefixExpr(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.OP_PREFIX_SEQ, false))
		{
			var seq = (String)ParseTools.consume(ctx).value;
			var prefixSeq = new AstExprOpPrefixSeq(seq, parsePrefixExpr(ctx));
			var proxy = new AstExprProxy(prefixSeq);
			ctx.lateParseExprs().prefixSeqs.put(prefixSeq, e -> proxy.nestedExpr = e);
			return proxy;
		}

		return parsePostfixExpr(ctx);
	}

	private static AstExpr parsePostfixExpr(ParseContext ctx)
	{
		var expr = parsePrimaryExpr(ctx);

		for(;;)
		{
			if(ParseTools.option(ctx, TokenType.OP_POSTFIX_SEQ, false))
			{
				var seq = (String)ParseTools.consume(ctx).value;
				var postfixSeq = new AstExprOpPostfixSeq(expr, seq);
				var proxy = new AstExprProxy(postfixSeq);
				ctx.lateParseExprs().postfixSeqs.put(postfixSeq, e -> proxy.nestedExpr = e);
				expr = proxy;
			}
			else if(ParseTools.option(ctx, TokenType.PUNCT_LPAREN, true))
			{
				expr = parseFunctionCall(ctx, expr, Inlining.AUTO);
				ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
			}
			else if(ParseTools.option(ctx, TokenType.PUNCT_LBRACKET, true))
			{
				var index = parse(ctx);
				expr = new AstExprIndexAccess(expr, index);
				ParseTools.expect(ctx, TokenType.PUNCT_RBRACKET, true);
			}
			else if(ParseTools.option(ctx, ".", true))
			{
				var global = ParseTools.option(ctx, TokenType.KW_GLOBAL, true);
				var name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
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
			var expr = parse(ctx);
			ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
			return new AstExprParens(expr);
		}

		if(ParseTools.option(ctx, TokenType.KW_GLOBAL, true))
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

		return parseMisc(ctx);
	}

	private static AstExpr parseConst(ParseContext ctx)
	{
		ParseTools.expect(ctx, TokenType.PUNCT_LPAREN, true);
		var expr = parse(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
		return new AstExprConst(expr);
	}

	private static AstExpr parseArrayLiteral(ParseContext ctx)
	{
		Maybe<AstType> type = Maybe.none();

		var state = ctx.recordState();

		try
		{
			type = Maybe.some(TypeParser.parse(ctx));
			ParseTools.expect(ctx, ":", true);
		}
		catch(CompileException ex)
		{
			ctx.backtrack(state);
		}

		var values = new ArrayList<AstExprLiteral>();

		if(!ParseTools.option(ctx, TokenType.PUNCT_RBRACKET, true))
		{
			var first = parse(ctx);

			if(!(first instanceof AstExprLiteral))
				throw new CompileException("array literal elements must be literals themselves");

			values.add((AstExprLiteral)first);

			while(ParseTools.option(ctx, TokenType.PUNCT_COMMA, true))
			{
				var next = parse(ctx);

				if(!(next instanceof AstExprLiteral))
					throw new CompileException("array literal elements must be literals themselves");

				values.add((AstExprLiteral)next);
			}

			ParseTools.expect(ctx, TokenType.PUNCT_RBRACKET, true);
		}

		return new AstExprLitArray(type, values);
	}

	private static AstExpr parseCast(ParseContext ctx, TokenType castType)
	{
		ParseTools.expect(ctx, TokenType.PUNCT_LBRACKET, true);
		var type = TypeParser.parse(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_RBRACKET, true);
		var expr = parsePrefixExpr(ctx);

		return switch(castType)
		{
		case KW_WIDEN_CAST   -> new AstExprWidenCast  (type, expr);
		case KW_NARROW_CAST  -> new AstExprNarrowCast (type, expr);
		case KW_SIGN_CAST    -> new AstExprSignCast   (type, expr);
		case KW_POINTER_CAST -> new AstExprPointerCast(type, expr);
		default -> throw new AssertionError("unreachable");
		};
	}

	private static Inlining parseInlineSpecifier(ParseContext ctx)
	{
		var token = ParseTools.consumeExpected(ctx, TokenType.LIT_BOOL);
		return (boolean)token.value ? Inlining.ALWAYS : Inlining.NEVER;
	}

	private static AstExpr parseMisc(ParseContext ctx)
	{
		var state = ctx.recordState();
		var token = ParseTools.consume(ctx);

		switch(token.type)
		{
		case KW_SIZEOF:
			if(ParseTools.option(ctx, TokenType.PUNCT_LBRACKET, true))
			{
				var type = TypeParser.parse(ctx);
				ParseTools.expect(ctx, TokenType.PUNCT_RBRACKET, true);
				return new AstExprSizeofType(type);
			}

			return new AstExprSizeofExpr(parsePrefixExpr(ctx));

		case KW_ALIGNOF:
			if(ParseTools.option(ctx, TokenType.PUNCT_LBRACKET, true))
			{
				var type = TypeParser.parse(ctx);
				ParseTools.expect(ctx, TokenType.PUNCT_RBRACKET, true);
				return new AstExprAlignofType(type);
			}

			return new AstExprAlignofExpr(parsePrefixExpr(ctx));

		case KW_OFFSETOF:
			return ParseTools.parenthesized(ctx, () ->
			{
				var type = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
				ParseTools.expect(ctx, TokenType.PUNCT_COMMA, true);
				var field = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
				return new AstExprOffsetof(type, field);
			});

		case KW_BUILTIN:
			return parseBuiltinCall(ctx);

		case KW_INLINE:
			var inline = ParseTools.parenthesized(ctx, () -> parseInlineSpecifier(ctx));
			var name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
			ParseTools.expect(ctx, TokenType.PUNCT_LPAREN, true);
			var call = parseFunctionCall(ctx, new AstExprNamedSymbol(name), inline);
			ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
			return call;

		case KW_NARROW_CAST:
		case KW_WIDEN_CAST:
		case KW_SIGN_CAST:
		case KW_POINTER_CAST:
			return parseCast(ctx, token.type);

		default: break;
		}

		ctx.backtrack(state);
		ParseTools.unexpectedToken(ctx);
		throw new AssertionError("unreachable");
	}

	private static AstExpr parseLiteral(ParseContext ctx)
	{
		var token = ParseTools.consume(ctx);

		return switch(token.type)
		{
		case LIT_NULL -> AstExprLitNull.INSTANCE;
		case LIT_BOOL -> AstExprLitBool.of((boolean)token.value);

		case LIT_INT   -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT));
		case LIT_INT8  -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT8));
		case LIT_INT16 -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT16));
		case LIT_INT32 -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT32));
		case LIT_INT64 -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.INT64));

		case LIT_UINT   -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT));
		case LIT_UINT8  -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT8));
		case LIT_UINT16 -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT16));
		case LIT_UINT32 -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT32));
		case LIT_UINT64 -> new AstExprLitInt((BigInteger)token.value, Maybe.some(BuiltinType.UINT64));

		case LIT_FLOAT32 -> new AstExprLitFloat((Fraction)token.value, Maybe.some(BuiltinType.FLOAT32));
		case LIT_FLOAT64 -> new AstExprLitFloat((Fraction)token.value, Maybe.some(BuiltinType.FLOAT64));

		case LIT_STRING -> new AstExprLitString((String)token.value);

		case LIT_INT_DEDUCE   -> new AstExprLitInt((BigInteger)token.value, Maybe.none());
		case LIT_FLOAT_DEDUCE -> new AstExprLitFloat((Fraction)token.value, Maybe.none());

		default -> throw new AssertionError("unreachable");
		};
	}

	private static AstExprBuiltinCall parseBuiltinCall(ParseContext ctx)
	{
		var path = ParseTools.path(ctx);
		var args = ParseTools.parenthesized(ctx, () -> parseArguments(ctx));
		return new AstExprBuiltinCall(path.toString(), args);
	}

	private static AstExprFunctionCall parseFunctionCall(ParseContext ctx, AstExpr expr, Inlining inline)
	{
		var args = parseArguments(ctx);
		return new AstExprFunctionCall(expr, args, inline);
	}

	private static List<AstExpr> parseArguments(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.PUNCT_RPAREN, false))
			return new ArrayList<>();

		return ParseTools.separated(ctx, TokenType.PUNCT_COMMA, () -> parse(ctx));
	}
}
