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
		if(ParseTools.option(scanner, Token.Type.STMT_IF, true))
			return parseIf(scanner);

		if(ParseTools.option(scanner, Token.Type.STMT_GOTO, true))
			return parseGoto(scanner);

		if(ParseTools.option(scanner, Token.Type.STMT_RETURN, true))
			return parseReturn(scanner);

		if(ParseTools.option(scanner, Token.Type.STMT_LABEL, false))
			return parseLabel(scanner);

		if(ParseTools.option(scanner, Token.Type.PUNCT_LBRACE, true))
			return parseCompound(scanner);

		return parseExprStmt(scanner);
	}

	private static If parseIf(Scanner scanner)
	{
		Expr condition = ParseTools.parenthesized(scanner, () -> ExprParser.parse(scanner));
		Stmt then = parse(scanner);

		boolean hasElse = ParseTools.option(scanner, Token.Type.STMT_ELSE, true);
		Optional<Stmt> otherwise = hasElse ? Optional.of(parse(scanner)) : Optional.empty();

		return new If(condition, then, otherwise);
	}

	private static Goto parseGoto(Scanner scanner)
	{
		String label = ParseTools.consumeExpected(scanner, Token.Type.STMT_LABEL).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Goto(label);
	}

	private static Return parseReturn(Scanner scanner)
	{
		Expr expr = ExprParser.parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Return(expr);
	}

	private static Label parseLabel(Scanner scanner)
	{
		String label = ParseTools.consume(scanner).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_COLON, true);
		return new Label(label);
	}

	private static Compound parseCompound(Scanner scanner)
	{
		Compound comp = new Compound();

		while(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACE, true))
			comp.body.add(parse(scanner));

		return comp;
	}

	private static ExprStmt parseExprStmt(Scanner scanner)
	{
		Expr expr = ExprParser.parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new ExprStmt(expr);
	}
}
