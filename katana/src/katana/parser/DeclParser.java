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
import katana.ast.DelayedExprParseList;
import katana.ast.decl.*;
import katana.ast.expr.AstExpr;
import katana.ast.expr.AstExprLiteral;
import katana.ast.stmt.AstStmt;
import katana.ast.type.AstType;
import katana.diag.CompileException;
import katana.op.Associativity;
import katana.op.BuiltinOps;
import katana.op.Kind;
import katana.op.Operator;
import katana.scanner.Scanner;
import katana.scanner.ScannerState;
import katana.scanner.Token;
import katana.utils.Maybe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class DeclParser
{
	public static AstDecl parse(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		boolean exported = ParseTools.option(scanner, Token.Type.DECL_EXPORT, true);
		boolean opaque = ParseTools.option(scanner, Token.Type.TYPE_OPAQUE, true);

		if(opaque && !exported)
			throw new CompileException("'opaque' must go after 'export'");

		Maybe<Maybe<String>> extern = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.DECL_EXTERN, true))
		{
			Maybe<String> externName = Maybe.none();

			if(ParseTools.option(scanner, Token.Type.LIT_STRING, false))
				externName = Maybe.some(ParseTools.consume(scanner).value);

			extern = Maybe.some(externName);
		}

		if(extern.isSome() && scanner.state().token.type != Token.Type.DECL_FN)
			throw new CompileException("extern can only be applied to overloads");

		switch(scanner.state().token.type)
		{
		case DECL_FN:     return parseFunction(scanner, exported, opaque, extern, delayedExprs);
		case DECL_DATA:   return parseStruct(scanner, exported, opaque, delayedExprs);
		case DECL_GLOBAL: return parseGlobal(scanner, exported, opaque, delayedExprs);
		case DECL_OP:     return parseOperator(scanner, exported);

		case DECL_IMPORT:
			if(exported)
				throw new CompileException("imports cannot be exported");

			return parseImport(scanner);

		case DECL_MODULE:
			if(exported)
				throw new CompileException("modules cannot be exported");

			return parseModule(scanner);

		case DECL_TYPE:
			if(opaque)
				throw new CompileException("type aliases cannot be exported opaquely");

			return parseTypeAlias(scanner, exported, delayedExprs);

		default: break;
		}

		ParseTools.unexpectedToken(scanner, Token.Category.DECL);
		throw new AssertionError("unreachable");
	}

	private static Kind parseOpKind(Scanner scanner)
	{
		if(ParseTools.option(scanner, Token.Type.DECL_PREFIX, true))
			return Kind.PREFIX;

		if(ParseTools.option(scanner, Token.Type.DECL_INFIX, true))
			return Kind.INFIX;

		else if(ParseTools.option(scanner, Token.Type.DECL_POSTFIX, true))
			return Kind.POSTFIX;

		ParseTools.unexpectedToken(scanner);
		throw new AssertionError("unreachable");
	}

	private static AstDecl parseOperator(Scanner scanner, boolean exported)
	{
		scanner.advance();

		String op = ParseTools.consumeExpected(scanner, Token.Category.OP).value;
		Kind kind = parseOpKind(scanner);

		if(BuiltinOps.find(op, kind).isSome())
		{
			String fmt = "redefinition of built-in operator '%s %s'";
			throw new CompileException(String.format(fmt, kind.toString().toLowerCase(), op));
		}

		if(kind == Kind.PREFIX)
		{
			ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
			return new AstDeclOperator(exported, Operator.prefix(op));
		}

		else if(kind == Kind.POSTFIX)
		{
			ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
			return new AstDeclOperator(exported, Operator.postfix(op));
		}

		Associativity assoc;

		if(ParseTools.option(scanner, "left", true))
			assoc = Associativity.LEFT;
		else if(ParseTools.option(scanner, "right", true))
			assoc = Associativity.RIGHT;
		else if(ParseTools.option(scanner, "none", true))
			assoc = Associativity.NONE;
		else
		{
			ParseTools.unexpectedToken(scanner);
			throw new AssertionError("unreachable");
		}

		String precStr = ParseTools.consumeExpected(scanner, Token.Type.LIT_INT_DEDUCE).value;
		BigInteger prec = new BigInteger(precStr);

		// precedence < 0 || precedence > 1000
		if(prec.compareTo(BigInteger.ZERO) == -1 || prec.compareTo(BigInteger.valueOf(1000)) == 1)
		{
			String fmt = "precedence for operator '%s' (%s) is out of range, valid values are from [0, 1000]";
			throw new CompileException(String.format(fmt, op, precStr));
		}

		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstDeclOperator(exported, Operator.infix(op, assoc, prec.intValue()));
	}

	private static AstDecl parseFunction(Scanner scanner, boolean exported, boolean opaque, Maybe<Maybe<String>> extern, DelayedExprParseList delayedExprs)
	{
		scanner.advance();

		String op = null;
		Kind kind = null;
		String name = null;

		if(ParseTools.option(scanner, Token.Category.OP, false))
		{
			op = ParseTools.consume(scanner).value;
			kind = parseOpKind(scanner);

			if(BuiltinOps.find(op, kind).isSome())
			{
				String fmt = "built-in operator '%s %s' cannot be overloaded";
				throw new CompileException(String.format(fmt, kind.toString().toLowerCase(), op));
			}
		}

		else
			name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;

		List<AstDeclFunction.Param> params = parseParameterList(scanner, delayedExprs);

		if((kind == Kind.PREFIX || kind == Kind.POSTFIX) && params.size() != 1)
			throw new CompileException("unary operator requires exactly one parameter");

		if(kind == Kind.INFIX && params.size() != 2)
			throw new CompileException("binary operator requires exactly two parameters");

		Maybe<AstType> ret = Maybe.none();

		if(ParseTools.option(scanner, "=>", true))
			ret = Maybe.some(TypeParser.parse(scanner, delayedExprs));

		if(extern.isSome())
		{
			ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
			return new AstDeclExternFunction(exported, opaque, extern.unwrap(), name, params, ret);
		}

		List<AstStmt> body = parseBody(scanner, delayedExprs);

		return name == null
			? new AstDeclDefinedOperator(exported, opaque, op, kind, params, ret, body)
			: new AstDeclDefinedFunction(exported, opaque, name, params, ret, body);
	}

	private static List<AstDeclFunction.Param> parseParameterList(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);

		List<AstDeclFunction.Param> params = new ArrayList<>();

		if(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, true))
		{
			params = ParseTools.separated(scanner, Token.Type.PUNCT_COMMA, () -> parseParameter(scanner, delayedExprs));
			ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		}

		return params;
	}

	private static AstDeclFunction.Param parseParameter(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		AstType type = TypeParser.parse(scanner, delayedExprs);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		return new AstDeclFunction.Param(type, name);
	}

	private static List<AstStmt> parseBody(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LBRACE, true);

		List<AstStmt> body = new ArrayList<>();

		while(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACE, false))
			body.add(StmtParser.parse(scanner, delayedExprs));

		ParseTools.expect(scanner, Token.Type.PUNCT_RBRACE, true);

		return body;
	}

	private static AstDeclStruct parseStruct(Scanner scanner, boolean exported, boolean opaque, DelayedExprParseList delayedExprs)
	{
		scanner.advance();

		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		boolean abiCompat = ParseTools.option(scanner, Token.Type.DECL_ABI, true);
		ParseTools.expect(scanner, Token.Type.PUNCT_LBRACE, true);

		List<AstDeclStruct.Field> fields = new ArrayList<>();

		while(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACE, false))
			fields.add(parseField(scanner, delayedExprs));

		ParseTools.expect(scanner, Token.Type.PUNCT_RBRACE, true);

		return new AstDeclStruct(exported, opaque, name, abiCompat, fields);
	}

	private static AstDeclStruct.Field parseField(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		AstType type = TypeParser.parse(scanner, delayedExprs);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstDeclStruct.Field(type, name);
	}

	private static Maybe<AstExprLiteral> parseGlobalInitAndScolon(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		if(ParseTools.option(scanner, Token.Type.MISC_UNDEF, true))
		{
			ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
			return Maybe.none();
		}

		AstExpr init = ExprParser.parse(scanner, delayedExprs);

		if(!(init instanceof AstExprLiteral))
			throw new CompileException("global initializer must be literal");

		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return Maybe.some((AstExprLiteral)init);
	}

	private static AstDeclGlobal parseGlobal(Scanner scanner, boolean exported, boolean opaque, DelayedExprParseList delayedExprs)
	{
		scanner.advance();

		ScannerState state = scanner.capture();

		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParseTools.consume(scanner).value;

			if(ParseTools.option(scanner, "=", true))
			{
				Maybe<AstExprLiteral> init = parseGlobalInitAndScolon(scanner, delayedExprs);
				return new AstDeclGlobal(exported, opaque, Maybe.none(), name, init);
			}
		}

		scanner.backtrack(state);

		AstType type = TypeParser.parse(scanner, delayedExprs);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, "=", true);

		Maybe<AstExprLiteral> init = parseGlobalInitAndScolon(scanner, delayedExprs);
		return new AstDeclGlobal(exported, opaque, Maybe.some(type), name, init);
	}

	private static AstDecl parseImport(Scanner scanner)
	{
		scanner.advance();

		AstPath path = ParseTools.path(scanner);

		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String rename = ParseTools.consume(scanner).value;
			ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
			return new AstDeclRenamedImport(path, rename);
		}

		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstDeclImport(path);
	}

	private static AstDeclModule parseModule(Scanner scanner)
	{
		scanner.advance();

		AstPath path = ParseTools.path(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstDeclModule(path);
	}

	private static AstDeclTypeAlias parseTypeAlias(Scanner scanner, boolean exported, DelayedExprParseList delayedExprs)
	{
		scanner.advance();
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, "=", true);
		AstType type = TypeParser.parse(scanner, delayedExprs);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstDeclTypeAlias(exported, name, type);
	}
}
