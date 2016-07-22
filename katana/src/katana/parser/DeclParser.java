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
		boolean exported = ParserTools.option(scanner, Token.Type.DECL_EXPORT, true);
		boolean opaque = ParserTools.option(scanner, Token.Type.TYPE_OPAQUE, true);

		if(opaque && !exported)
			throw new RuntimeException("'opaque' must go after 'export'");

		switch(scanner.token().type)
		{
		case DECL_FN:
			if(opaque)
				throw new RuntimeException("functions cannot be exported opaquely");

			return parseFunction(scanner, exported);

		case DECL_DATA:
			return parseData(scanner, exported, opaque);

		case DECL_VAR:
			return parseVariable(scanner, exported, opaque);

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

		ParserTools.unexpectedToken(scanner, Token.Category.DECL);
		throw new AssertionError("unreachable");
	}

	private static Function parseFunction(Scanner scanner, boolean exported)
	{
		scanner.advance();
		String name = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
		ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);

		ArrayList<Function.Parameter> params = new ArrayList<>();

		if(!ParserTools.option(scanner, Token.Type.PUNCT_RPAREN, true))
		{
			do
			{
				Type ptype = TypeParser.parse(scanner);
				String pname = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
				params.add(new Function.Parameter(ptype, pname));
			}

			while(ParserTools.option(scanner, Token.Type.PUNCT_COMMA, true));
		}

		ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		ParserTools.expect(scanner, Token.Type.PUNCT_LBRACE, true);

		ArrayList<Stmt> body = new ArrayList<>();

		while(!ParserTools.option(scanner, Token.Type.PUNCT_RBRACE, true))
			body.add(StmtParser.parse(scanner));

		return new Function(exported, name, params, body);
	}

	private static Data parseData(Scanner scanner, boolean exported, boolean opaque)
	{
		return null;
	}

	private static Variable parseVariable(Scanner scanner, boolean exported, boolean opaque)
	{
		String name = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
		ParserTools.expect(scanner, PUNCT_);
		return null;
	}

	private static Import parseImport(Scanner scanner)
	{
		scanner.advance();
		Path path = parsePath(scanner);

		Optional<String> rename = Optional.empty();

		if(ParserTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParserTools.consume(scanner).value;
			rename = Optional.of(name);
		}

		ParserTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);

		return new Import(path, rename);
	}

	private static Module parseModule(Scanner scanner)
	{
		scanner.advance();
		Path path = parsePath(scanner);
		ParserTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Module(path);
	}

	private static Path parsePath(Scanner scanner)
	{
		ArrayList<String> components = new ArrayList<>();

		String first = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
		components.add(first);

		while(ParserTools.option(scanner, Token.Type.PUNCT_DOT, true))
		{
			String next = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
			components.add(next);
		}

		return new Path(components);
	}
}
