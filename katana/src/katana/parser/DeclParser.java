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

import katana.ast.Decl;
import katana.ast.Path;
import katana.ast.Stmt;
import katana.ast.Type;
import katana.ast.decl.*;
import katana.scanner.Scanner;
import katana.scanner.Token;
import katana.utils.Maybe;

import java.util.ArrayList;

public class DeclParser
{
	public static Decl parse(Scanner scanner)
	{
		boolean exported = ParseTools.option(scanner, Token.Type.DECL_EXPORT, true);
		boolean opaque = ParseTools.option(scanner, Token.Type.TYPE_OPAQUE, true);

		if(opaque && !exported)
			throw new RuntimeException("'opaque' must go after 'export'");

		Maybe<String> extern = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.DECL_EXTERN, true))
			extern = Maybe.some(ParseTools.consumeExpected(scanner, Token.Type.LIT_STRING).value);

		if(extern.isSome() && scanner.token().type != Token.Type.DECL_FN)
			throw new RuntimeException("extern can only be applied to functions");

		switch(scanner.token().type)
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

		default: break;
		}

		ParseTools.unexpectedToken(scanner, Token.Category.DECL);
		throw new AssertionError("unreachable");
	}

	private static Decl parseFunction(Scanner scanner, boolean exported, boolean opaque, Maybe<String> extern)
	{
		scanner.advance();

		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ArrayList<Function.Param> params = parseParameterList(scanner);

		Maybe<Type> ret = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.PUNCT_RET, true))
			ret = Maybe.some(TypeParser.parse(scanner));

		if(extern.isSome())
		{
			ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
			return new ExternFunction(exported, opaque, extern.unwrap(), name, params, ret);
		}

		ArrayList<Stmt> body = parseBody(scanner);
		return new Function(exported, opaque, name, params, ret, body);
	}

	private static ArrayList<Function.Param> parseParameterList(Scanner scanner)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);

		ArrayList<Function.Param> params = new ArrayList<>();

		if(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, true))
		{
			params = ParseTools.separated(scanner, Token.Type.PUNCT_COMMA, () -> parseParameter(scanner));
			ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		}

		return params;
	}

	private static Function.Param parseParameter(Scanner scanner)
	{
		Type type = TypeParser.parse(scanner);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		return new Function.Param(type, name);
	}

	private static ArrayList<Stmt> parseBody(Scanner scanner)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LBRACE, true);

		ArrayList<Stmt> body = new ArrayList<>();

		while(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACE, false))
			body.add(StmtParser.parse(scanner));

		ParseTools.expect(scanner, Token.Type.PUNCT_RBRACE, true);

		return body;
	}

	private static Data parseData(Scanner scanner, boolean exported, boolean opaque)
	{
		scanner.advance();

		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_LBRACE, true);

		ArrayList<Data.Field> fields = new ArrayList<>();

		while(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACE, false))
			fields.add(parseField(scanner));

		ParseTools.expect(scanner, Token.Type.PUNCT_RBRACE, true);

		return new Data(exported, opaque, name, fields);
	}

	private static Data.Field parseField(Scanner scanner)
	{
		Type type = TypeParser.parse(scanner);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Data.Field(type, name);
	}

	private static Global parseGlobal(Scanner scanner, boolean exported, boolean opaque)
	{
		scanner.advance();

		Type type = TypeParser.parse(scanner);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Global(exported, opaque, type, name);
	}

	private static Import parseImport(Scanner scanner)
	{
		scanner.advance();

		Path path = ParseTools.path(scanner);
		Maybe<String> rename = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParseTools.consume(scanner).value;
			rename = Maybe.some(name);
		}

		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);

		return new Import(path, rename);
	}

	private static Module parseModule(Scanner scanner)
	{
		scanner.advance();

		Path path = ParseTools.path(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Module(path);
	}
}
