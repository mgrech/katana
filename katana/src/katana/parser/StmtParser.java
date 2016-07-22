package katana.parser;

import katana.ast.Expr;
import katana.ast.Stmt;
import katana.ast.stmt.*;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.util.Optional;

public class StmtParser
{
	public static Stmt parse(Scanner scanner)
	{
		if(ParserTools.option(scanner, Token.Type.STMT_IF, true))
			return parseIf(scanner);

		if(ParserTools.option(scanner, Token.Type.STMT_GOTO, true))
			return parseGoto(scanner);

		if(ParserTools.option(scanner, Token.Type.STMT_RETURN, true))
			return parseReturn(scanner);

		if(ParserTools.option(scanner, Token.Type.STMT_LABEL, false))
			return parseLabel(scanner);

		if(ParserTools.option(scanner, Token.Type.PUNCT_LBRACE, true))
			return parseCompound(scanner);

		return parseExprStmt(scanner);
	}

	private static If parseIf(Scanner scanner)
	{
		ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		Expr condition = ExprParser.parse(scanner);
		ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		Stmt then = parse(scanner);

		boolean hasElse = ParserTools.option(scanner, Token.Type.STMT_ELSE, true);
		Optional<Stmt> otherwise = hasElse ? Optional.of(parse(scanner)) : Optional.empty();

		return new If(condition, then, otherwise);
	}

	private static Goto parseGoto(Scanner scanner)
	{
		String label = ParserTools.expectAndConsume(scanner, Token.Type.STMT_LABEL).value;
		ParserTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Goto(label);
	}

	private static Return parseReturn(Scanner scanner)
	{
		Expr expr = ExprParser.parse(scanner);
		ParserTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Return(expr);
	}

	private static Label parseLabel(Scanner scanner)
	{
		String label = ParserTools.consume(scanner).value;
		ParserTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Label(label);
	}

	private static Compound parseCompound(Scanner scanner)
	{
		Compound comp = new Compound();

		while(!ParserTools.option(scanner, Token.Type.PUNCT_RBRACE, true))
			comp.body.add(parse(scanner));

		return comp;
	}

	private static ExprStmt parseExprStmt(Scanner scanner)
	{
		Expr expr = ExprParser.parse(scanner);
		ParserTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new ExprStmt(expr);
	}
}
