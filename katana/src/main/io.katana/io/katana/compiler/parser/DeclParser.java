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

package io.katana.compiler.parser;

import io.katana.compiler.ast.AstPath;
import io.katana.compiler.ast.decl.*;
import io.katana.compiler.ast.expr.AstExpr;
import io.katana.compiler.ast.expr.AstExprLiteral;
import io.katana.compiler.ast.stmt.AstStmt;
import io.katana.compiler.ast.type.AstType;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.op.Associativity;
import io.katana.compiler.op.BuiltinOps;
import io.katana.compiler.op.Kind;
import io.katana.compiler.op.Operator;
import io.katana.compiler.scanner.TokenCategory;
import io.katana.compiler.scanner.TokenType;
import io.katana.compiler.utils.Maybe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class DeclParser
{
	public static AstDecl parse(ParseContext ctx)
	{
		boolean exported = ParseTools.option(ctx, TokenType.DECL_EXPORT, true);
		boolean opaque = ParseTools.option(ctx, TokenType.TYPE_OPAQUE, true);

		if(opaque && !exported)
			throw new CompileException("'opaque' must go after 'export'");

		Maybe<Maybe<String>> extern = Maybe.none();

		if(ParseTools.option(ctx, TokenType.DECL_EXTERN, true))
		{
			Maybe<String> externName = Maybe.none();

			if(ParseTools.option(ctx, TokenType.LIT_STRING, false))
				externName = Maybe.some(ParseTools.consume(ctx).value);

			extern = Maybe.some(externName);
		}

		if(extern.isSome() && ctx.token().type != TokenType.DECL_FN)
			throw new CompileException("extern can only be applied to overloads");

		switch(ctx.token().type)
		{
		case DECL_FN:     return parseFunction(ctx, exported, opaque, extern);
		case DECL_DATA:   return parseStruct(ctx, exported, opaque);
		case DECL_GLOBAL: return parseGlobal(ctx, exported, opaque);
		case DECL_OP:     return parseOperator(ctx, exported);

		case DECL_IMPORT:
			if(exported)
				throw new CompileException("imports cannot be exported");

			return parseImport(ctx);

		case DECL_MODULE:
			if(exported)
				throw new CompileException("modules cannot be exported");

			return parseModule(ctx);

		case DECL_TYPE:
			if(opaque)
				throw new CompileException("type aliases cannot be exported opaquely");

			return parseTypeAlias(ctx, exported);

		default: break;
		}

		ParseTools.unexpectedToken(ctx, TokenCategory.DECL);
		throw new AssertionError("unreachable");
	}

	private static Kind parseOpKind(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.DECL_PREFIX, true))
			return Kind.PREFIX;

		if(ParseTools.option(ctx, TokenType.DECL_INFIX, true))
			return Kind.INFIX;

		else if(ParseTools.option(ctx, TokenType.DECL_POSTFIX, true))
			return Kind.POSTFIX;

		ParseTools.unexpectedToken(ctx);
		throw new AssertionError("unreachable");
	}

	private static AstDecl parseOperator(ParseContext ctx, boolean exported)
	{
		ctx.advance();

		String op = ParseTools.consumeExpected(ctx, TokenCategory.OP).value;
		Kind kind = parseOpKind(ctx);

		if(BuiltinOps.find(op, kind).isSome())
		{
			String fmt = "redefinition of built-in operator '%s %s'";
			throw new CompileException(String.format(fmt, kind.toString().toLowerCase(), op));
		}

		if(kind == Kind.PREFIX)
		{
			ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
			return new AstDeclOperator(exported, Operator.prefix(op));
		}

		else if(kind == Kind.POSTFIX)
		{
			ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
			return new AstDeclOperator(exported, Operator.postfix(op));
		}

		Associativity assoc;

		if(ParseTools.option(ctx, "left", true))
			assoc = Associativity.LEFT;
		else if(ParseTools.option(ctx, "right", true))
			assoc = Associativity.RIGHT;
		else if(ParseTools.option(ctx, "none", true))
			assoc = Associativity.NONE;
		else
		{
			ParseTools.unexpectedToken(ctx);
			throw new AssertionError("unreachable");
		}

		String precStr = ParseTools.consumeExpected(ctx, TokenType.LIT_INT_DEDUCE).value;
		BigInteger prec = new BigInteger(precStr);

		// precedence < 0 || precedence > 1000
		if(prec.compareTo(BigInteger.ZERO) == -1 || prec.compareTo(BigInteger.valueOf(1000)) == 1)
		{
			String fmt = "precedence for operator '%s' (%s) is out of range, valid values are from [0, 1000]";
			throw new CompileException(String.format(fmt, op, precStr));
		}

		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclOperator(exported, Operator.infix(op, assoc, prec.intValue()));
	}

	private static AstDecl parseFunction(ParseContext ctx, boolean exported, boolean opaque, Maybe<Maybe<String>> extern)
	{
		ctx.advance();

		String op = null;
		Kind kind = null;
		String name = null;

		if(ParseTools.option(ctx, TokenCategory.OP, false))
		{
			op = ParseTools.consume(ctx).value;
			kind = parseOpKind(ctx);

			if(BuiltinOps.find(op, kind).isSome())
			{
				String fmt = "built-in operator '%s %s' cannot be overloaded";
				throw new CompileException(String.format(fmt, kind.toString().toLowerCase(), op));
			}
		}

		else
			name = ParseTools.consumeExpected(ctx, TokenType.IDENT).value;

		List<AstDeclFunction.Param> params = parseParameterList(ctx);

		if((kind == Kind.PREFIX || kind == Kind.POSTFIX) && params.size() != 1)
			throw new CompileException("unary operator requires exactly one parameter");

		if(kind == Kind.INFIX && params.size() != 2)
			throw new CompileException("binary operator requires exactly two parameters");

		Maybe<AstType> ret = Maybe.none();

		if(ParseTools.option(ctx, "=>", true))
			ret = Maybe.some(TypeParser.parse(ctx));

		if(extern.isSome())
		{
			ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
			return new AstDeclExternFunction(exported, opaque, extern.unwrap(), name, params, ret);
		}

		List<AstStmt> body = parseBody(ctx);

		return name == null
			? new AstDeclDefinedOperator(exported, opaque, op, kind, params, ret, body)
			: new AstDeclDefinedFunction(exported, opaque, name, params, ret, body);
	}

	private static List<AstDeclFunction.Param> parseParameterList(ParseContext ctx)
	{
		ParseTools.expect(ctx, TokenType.PUNCT_LPAREN, true);

		List<AstDeclFunction.Param> params = new ArrayList<>();

		if(!ParseTools.option(ctx, TokenType.PUNCT_RPAREN, true))
		{
			params = ParseTools.separated(ctx, TokenType.PUNCT_COMMA, () -> parseParameter(ctx));
			ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
		}

		return params;
	}

	private static AstDeclFunction.Param parseParameter(ParseContext ctx)
	{
		AstType type = TypeParser.parse(ctx);
		String name = ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		return new AstDeclFunction.Param(type, name);
	}

	private static List<AstStmt> parseBody(ParseContext ctx)
	{
		ParseTools.expect(ctx, TokenType.PUNCT_LBRACE, true);

		List<AstStmt> body = new ArrayList<>();

		while(!ParseTools.option(ctx, TokenType.PUNCT_RBRACE, false))
			body.add(StmtParser.parse(ctx));

		ParseTools.expect(ctx, TokenType.PUNCT_RBRACE, true);

		return body;
	}

	private static AstDeclStruct parseStruct(ParseContext ctx, boolean exported, boolean opaque)
	{
		ctx.advance();

		String name = ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		boolean abiCompat = ParseTools.option(ctx, TokenType.DECL_ABI, true);
		ParseTools.expect(ctx, TokenType.PUNCT_LBRACE, true);

		List<AstDeclStruct.Field> fields = new ArrayList<>();

		while(!ParseTools.option(ctx, TokenType.PUNCT_RBRACE, false))
			fields.add(parseField(ctx));

		ParseTools.expect(ctx, TokenType.PUNCT_RBRACE, true);

		return new AstDeclStruct(exported, opaque, name, abiCompat, fields);
	}

	private static AstDeclStruct.Field parseField(ParseContext ctx)
	{
		AstType type = TypeParser.parse(ctx);
		String name = ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclStruct.Field(type, name);
	}

	private static Maybe<AstExprLiteral> parseGlobalInitAndScolon(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.MISC_UNDEF, true))
		{
			ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
			return Maybe.none();
		}

		AstExpr init = ExprParser.parse(ctx);

		if(!(init instanceof AstExprLiteral))
			throw new CompileException("global initializer must be literal");

		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return Maybe.some((AstExprLiteral)init);
	}

	private static AstDeclGlobal parseGlobal(ParseContext ctx, boolean exported, boolean opaque)
	{
		ctx.advance();

		ParseContext tmp = ctx.clone();

		if(ParseTools.option(ctx, TokenType.IDENT, false))
		{
			String name = ParseTools.consume(ctx).value;

			if(ParseTools.option(ctx, "=", true))
			{
				Maybe<AstExprLiteral> init = parseGlobalInitAndScolon(ctx);
				return new AstDeclGlobal(exported, opaque, Maybe.none(), name, init);
			}
		}

		ctx.backtrack(tmp);

		AstType type = TypeParser.parse(ctx);
		String name = ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		ParseTools.expect(ctx, "=", true);

		Maybe<AstExprLiteral> init = parseGlobalInitAndScolon(ctx);
		return new AstDeclGlobal(exported, opaque, Maybe.some(type), name, init);
	}

	private static AstDecl parseImport(ParseContext ctx)
	{
		ctx.advance();

		AstPath path = ParseTools.path(ctx);

		if(ParseTools.option(ctx, TokenType.IDENT, false))
		{
			String rename = ParseTools.consume(ctx).value;
			ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
			return new AstDeclRenamedImport(path, rename);
		}

		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclImport(path);
	}

	private static AstDeclModule parseModule(ParseContext ctx)
	{
		ctx.advance();

		AstPath path = ParseTools.path(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclModule(path);
	}

	private static AstDeclTypeAlias parseTypeAlias(ParseContext ctx, boolean exported)
	{
		ctx.advance();
		String name = ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		ParseTools.expect(ctx, "=", true);
		AstType type = TypeParser.parse(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclTypeAlias(exported, name, type);
	}
}