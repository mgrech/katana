package katana.parser;

import katana.ast.Decl;
import katana.ast.Path;
import katana.ast.Stmt;
import katana.ast.Type;
import katana.ast.decl.*;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.util.ArrayList;
import java.util.Optional;

public class DeclParser
{
	public static Decl parse(Scanner scanner)
	{
		boolean exported = ParseTools.option(scanner, Token.Type.DECL_EXPORT, true);
		boolean opaque = ParseTools.option(scanner, Token.Type.TYPE_OPAQUE, true);

		if(opaque && !exported)
			throw new RuntimeException("'opaque' must go after 'export'");

		switch(scanner.token().type)
		{
		case DECL_FN:     return parseFunction(scanner, exported, opaque);
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

	private static Function parseFunction(Scanner scanner, boolean exported, boolean opaque)
	{
		scanner.advance();

		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ArrayList<Function.Parameter> params = parseParameterList(scanner);

		Optional<Type> ret = Optional.empty();

		if(ParseTools.option(scanner, Token.Type.PUNCT_RET, true))
			ret = Optional.of(TypeParser.parse(scanner));

		ArrayList<Function.Local> locals = parseLocalList(scanner);
		ArrayList<Stmt> body = parseBody(scanner);
		return new Function(exported, opaque, name, params, ret, locals, body);
	}

	private static ArrayList<Function.Parameter> parseParameterList(Scanner scanner)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);

		ArrayList<Function.Parameter> params = new ArrayList<>();

		if(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, true))
		{
			params = ParseTools.separated(scanner, Token.Type.PUNCT_COMMA, () -> parseParameter(scanner));
			ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		}

		return params;
	}

	private static Function.Parameter parseParameter(Scanner scanner)
	{
		Type type = TypeParser.parse(scanner);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		return new Function.Parameter(type, name);
	}

	private static ArrayList<Function.Local> parseLocalList(Scanner scanner)
	{
		ArrayList<Function.Local> locals = new ArrayList<>();

		while(!ParseTools.option(scanner, Token.Type.PUNCT_LBRACE, false))
			locals.add(parseLocal(scanner));

		return locals;
	}

	private static Function.Local parseLocal(Scanner scanner)
	{
		Type type = TypeParser.parse(scanner);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Function.Local(type, name);
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
		Optional<String> rename = Optional.empty();

		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParseTools.consume(scanner).value;
			rename = Optional.of(name);
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
