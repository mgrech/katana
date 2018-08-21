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

import io.katana.compiler.ast.decl.*;
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
		var exported = ParseTools.option(ctx, TokenType.KW_EXPORT, true);
		var opaque = ParseTools.option(ctx, TokenType.KW_OPAQUE, true);

		if(opaque && !exported)
			throw new CompileException("'opaque' must go after 'export'");

		Maybe<Maybe<String>> extern = Maybe.none();

		if(ParseTools.option(ctx, TokenType.KW_EXTERN, true))
		{
			Maybe<String> externName = Maybe.none();

			if(ParseTools.option(ctx, TokenType.LIT_STRING, false))
				externName = Maybe.some((String)ParseTools.consume(ctx).value);

			extern = Maybe.some(externName);
		}

		if(extern.isSome() && ctx.token().type != TokenType.KW_FN)
			throw new CompileException("extern can only be applied to overloads");

		switch(ctx.token().type)
		{
		case KW_FN:       return parseFunction(ctx, exported, opaque, extern);
		case KW_DATA:     return parseStruct(ctx, exported, opaque);
		case KW_GLOBAL:   return parseGlobal(ctx, exported, opaque);
		case KW_OPERATOR: return parseOperator(ctx, exported);

		case KW_IMPORT:
			if(exported)
				throw new CompileException("imports cannot be exported");

			return parseImport(ctx);

		case KW_MODULE:
			if(exported)
				throw new CompileException("modules cannot be exported");

			return parseModule(ctx);

		case KW_TYPE:
			if(opaque)
				throw new CompileException("type aliases cannot be exported opaquely");

			return parseTypeAlias(ctx, exported);

		default: break;
		}

		ParseTools.unexpectedToken(ctx);
		throw new AssertionError("unreachable");
	}

	private static Kind parseOpKind(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.KW_PREFIX, true))
			return Kind.PREFIX;

		if(ParseTools.option(ctx, TokenType.KW_INFIX, true))
			return Kind.INFIX;

		else if(ParseTools.option(ctx, TokenType.KW_POSTFIX, true))
			return Kind.POSTFIX;

		ParseTools.unexpectedToken(ctx);
		throw new AssertionError("unreachable");
	}

	private static AstDecl parseOperator(ParseContext ctx, boolean exported)
	{
		ctx.advance();

		var op = (String)ParseTools.consumeExpected(ctx, TokenCategory.OP).value;
		var kind = parseOpKind(ctx);

		if(BuiltinOps.find(op, kind).isSome())
		{
			var fmt = "redefinition of built-in operator '%s %s'";
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

		var precedence = (BigInteger)ParseTools.consumeExpected(ctx, TokenType.LIT_INT_DEDUCE).value;

		// precedence < 0 || precedence > 1000
		if(precedence.compareTo(BigInteger.ZERO) == -1 || precedence.compareTo(BigInteger.valueOf(1000)) == 1)
		{
			var fmt = "precedence for operator '%s' (%s) is out of range, valid values are from [0, 1000]";
			throw new CompileException(String.format(fmt, op, precedence));
		}

		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclOperator(exported, Operator.infix(op, assoc, precedence.intValue()));
	}

	private static AstDecl parseFunction(ParseContext ctx, boolean exported, boolean opaque, Maybe<Maybe<String>> extern)
	{
		ctx.advance();

		String op = null;
		Kind kind = null;
		String name = null;

		if(ParseTools.option(ctx, TokenCategory.OP, false))
		{
			op = (String)ParseTools.consume(ctx).value;
			kind = parseOpKind(ctx);

			if(BuiltinOps.find(op, kind).isSome())
			{
				String fmt = "built-in operator '%s %s' cannot be overloaded";
				throw new CompileException(String.format(fmt, kind.toString().toLowerCase(), op));
			}
		}
		else
			name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;

		var params = parseParameterList(ctx);

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

		var body = parseBody(ctx);

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
		var type = TypeParser.parse(ctx);
		var name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		return new AstDeclFunction.Param(type, name);
	}

	private static List<AstStmt> parseBody(ParseContext ctx)
	{
		ParseTools.expect(ctx, TokenType.PUNCT_LBRACE, true);

		var body = new ArrayList<AstStmt>();

		while(!ParseTools.option(ctx, TokenType.PUNCT_RBRACE, false))
			body.add(StmtParser.parse(ctx));

		ParseTools.expect(ctx, TokenType.PUNCT_RBRACE, true);

		return body;
	}

	private static AstDeclStruct parseStruct(ParseContext ctx, boolean exported, boolean opaque)
	{
		ctx.advance();

		var name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		var abiCompat = ParseTools.option(ctx, TokenType.KW_ABI, true);
		ParseTools.expect(ctx, TokenType.PUNCT_LBRACE, true);

		var fields = new ArrayList<AstDeclStruct.Field>();

		while(!ParseTools.option(ctx, TokenType.PUNCT_RBRACE, false))
			fields.add(parseField(ctx));

		ParseTools.expect(ctx, TokenType.PUNCT_RBRACE, true);

		return new AstDeclStruct(exported, opaque, name, abiCompat, fields);
	}

	private static AstDeclStruct.Field parseField(ParseContext ctx)
	{
		var type = TypeParser.parse(ctx);
		var name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclStruct.Field(type, name);
	}

	private static Maybe<AstExprLiteral> parseGlobalInitAndScolon(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.KW_UNDEF, true))
		{
			ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
			return Maybe.none();
		}

		var init = ExprParser.parse(ctx);

		if(!(init instanceof AstExprLiteral))
			throw new CompileException("global initializer must be literal");

		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return Maybe.some((AstExprLiteral)init);
	}

	private static AstDeclGlobal parseGlobal(ParseContext ctx, boolean exported, boolean opaque)
	{
		ctx.advance();

		var tmp = ctx.clone();

		if(ParseTools.option(ctx, TokenType.IDENT, false))
		{
			var name = (String)ParseTools.consume(ctx).value;

			if(ParseTools.option(ctx, "=", true))
			{
				var init = parseGlobalInitAndScolon(ctx);
				return new AstDeclGlobal(exported, opaque, Maybe.none(), name, init);
			}
		}

		ctx.backtrack(tmp);

		var type = TypeParser.parse(ctx);
		var name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		ParseTools.expect(ctx, "=", true);

		var init = parseGlobalInitAndScolon(ctx);
		return new AstDeclGlobal(exported, opaque, Maybe.some(type), name, init);
	}

	private static AstDecl parseImport(ParseContext ctx)
	{
		ctx.advance();

		var path = ParseTools.path(ctx);

		if(ParseTools.option(ctx, TokenType.IDENT, false))
		{
			var rename = (String)ParseTools.consume(ctx).value;
			ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
			return new AstDeclRenamedImport(path, rename);
		}

		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclImport(path);
	}

	private static AstDeclModule parseModule(ParseContext ctx)
	{
		ctx.advance();

		var path = ParseTools.path(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclModule(path);
	}

	private static AstDeclTypeAlias parseTypeAlias(ParseContext ctx, boolean exported)
	{
		ctx.advance();
		var name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		ParseTools.expect(ctx, "=", true);
		var type = TypeParser.parse(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstDeclTypeAlias(exported, name, type);
	}
}
