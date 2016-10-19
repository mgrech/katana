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

import katana.ast.AstPath;
import katana.ast.decl.*;
import katana.ast.expr.AstExpr;
import katana.ast.expr.AstExprLiteral;
import katana.ast.stmt.AstStmt;
import katana.ast.type.AstType;
import katana.scanner.Scanner;
import katana.scanner.ScannerState;
import katana.scanner.Token;
import katana.utils.Maybe;

import java.util.ArrayList;
import java.util.List;

public class DeclParser
{
	public static AstDecl parse(Scanner scanner)
	{
		boolean exported = ParseTools.option(scanner, Token.Type.DECL_EXPORT, true);
		boolean opaque = ParseTools.option(scanner, Token.Type.TYPE_OPAQUE, true);

		if(opaque && !exported)
			throw new RuntimeException("'opaque' must go after 'export'");

		Maybe<String> extern = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.DECL_EXTERN, true))
			extern = Maybe.some(ParseTools.consumeExpected(scanner, Token.Type.LIT_STRING).value);

		if(extern.isSome() && scanner.state().token.type != Token.Type.DECL_FN)
			throw new RuntimeException("extern can only be applied to overloads");

		switch(scanner.state().token.type)
		{
		case DECL_FN:     return parseFunction(scanner, exported, opaque, extern);
		case DECL_DATA:   return parseData(scanner, exported, opaque);
		case DECL_GLOBAL: return parseGlobal(scanner, exported, opaque);

		case DECL_IMPORT:
			if(exported)
				throw new RuntimeException("imports cannot be exported");

			return parseImport(scanner);

		case DECL_MODULE:
			if(exported)
				throw new RuntimeException("modules cannot be exported");

			return parseModule(scanner);

		case DECL_TYPE:
			if(opaque)
				throw new RuntimeException("type aliases cannot be exported opaquely");

			return parseTypeAlias(scanner, exported);

		default: break;
		}

		ParseTools.unexpectedToken(scanner, Token.Category.DECL);
		throw new AssertionError("unreachable");
	}

	private static AstDecl parseFunction(Scanner scanner, boolean exported, boolean opaque, Maybe<String> extern)
	{
		scanner.advance();

		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		List<AstDeclFunction.Param> params = parseParameterList(scanner);

		Maybe<AstType> ret = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.PUNCT_RET, true))
			ret = Maybe.some(TypeParser.parse(scanner));

		if(extern.isSome())
		{
			ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
			return new AstDeclExternFunction(exported, opaque, extern.unwrap(), name, params, ret);
		}

		List<AstStmt> body = parseBody(scanner);
		return new AstDeclDefinedFunction(exported, opaque, name, params, ret, body);
	}

	private static List<AstDeclFunction.Param> parseParameterList(Scanner scanner)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);

		List<AstDeclFunction.Param> params = new ArrayList<>();

		if(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, true))
		{
			params = ParseTools.separated(scanner, Token.Type.PUNCT_COMMA, () -> parseParameter(scanner));
			ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		}

		return params;
	}

	private static AstDeclFunction.Param parseParameter(Scanner scanner)
	{
		AstType type = TypeParser.parse(scanner);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		return new AstDeclFunction.Param(type, name);
	}

	private static List<AstStmt> parseBody(Scanner scanner)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LBRACE, true);

		List<AstStmt> body = new ArrayList<>();

		while(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACE, false))
			body.add(StmtParser.parse(scanner));

		ParseTools.expect(scanner, Token.Type.PUNCT_RBRACE, true);

		return body;
	}

	private static AstDeclData parseData(Scanner scanner, boolean exported, boolean opaque)
	{
		scanner.advance();

		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_LBRACE, true);

		List<AstDeclData.Field> fields = new ArrayList<>();

		while(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACE, false))
			fields.add(parseField(scanner));

		ParseTools.expect(scanner, Token.Type.PUNCT_RBRACE, true);

		return new AstDeclData(exported, opaque, name, fields);
	}

	private static AstDeclData.Field parseField(Scanner scanner)
	{
		AstType type = TypeParser.parse(scanner);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstDeclData.Field(type, name);
	}

	private static AstDeclGlobal parseGlobal(Scanner scanner, boolean exported, boolean opaque)
	{
		scanner.advance();

		ScannerState state = scanner.capture();

		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParseTools.consume(scanner).value;

			if(ParseTools.option(scanner, Token.Type.PUNCT_ASSIGN, true))
			{
				AstExpr init = ExprParser.parse(scanner);

				if(!(init instanceof AstExprLiteral))
					throw new RuntimeException("global initializer must be literal");

				ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
				return new AstDeclGlobal(exported, opaque, Maybe.none(), name, (AstExprLiteral)init);
			}
		}

		scanner.backtrack(state);

		AstType type = TypeParser.parse(scanner);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_ASSIGN, true);
		AstExpr init = ExprParser.parse(scanner);

		if(!(init instanceof AstExprLiteral))
			throw new RuntimeException("global initializer must be literal");

		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstDeclGlobal(exported, opaque, Maybe.some(type), name, (AstExprLiteral)init);
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

	private static AstDeclTypeAlias parseTypeAlias(Scanner scanner, boolean exported)
	{
		scanner.advance();
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_ASSIGN, true);
		AstType type = TypeParser.parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstDeclTypeAlias(exported, name, type);
	}
}
