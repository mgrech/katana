package katana.parser;

import katana.Maybe;
import katana.ast.Expr;
import katana.ast.Stmt;
import katana.ast.stmt.*;
import katana.scanner.Scanner;
import katana.scanner.Token;

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

		if(ParseTools.option(scanner, Token.Type.STMT_LOOP, true))
			return parseLoop(scanner);

		if(ParseTools.option(scanner, Token.Type.STMT_WHILE, true))
			return parseWhile(scanner);

		if(ParseTools.option(scanner, Token.Type.STMT_LABEL, false))
			return parseLabel(scanner);

		if(ParseTools.option(scanner, Token.Type.PUNCT_LBRACE, true))
			return parseCompound(scanner);

		return parseExprStmt(scanner);
	}

	private static Stmt parseIf(Scanner scanner)
	{
		boolean negated = ParseTools.option(scanner, Token.Type.STMT_NEGATE, true);
		Expr condition = ParseTools.parenthesized(scanner, () -> ExprParser.parse(scanner));
		Stmt then = parse(scanner);

		Maybe<Stmt> else_ = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.STMT_ELSE, true))
			else_ = Maybe.some(parse(scanner));

		if(else_.isNone())
			return new If(negated, condition, then);

		return new IfElse(negated, condition, then, else_.unwrap());
	}

	private static Goto parseGoto(Scanner scanner)
	{
		String label = ParseTools.consumeExpected(scanner, Token.Type.STMT_LABEL).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Goto(label);
	}

	private static Return parseReturn(Scanner scanner)
	{
		if(ParseTools.option(scanner, Token.Type.PUNCT_SCOLON, true))
			return new Return(Maybe.none());

		Expr expr = ExprParser.parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new Return(Maybe.some(expr));
	}

	private static Loop parseLoop(Scanner scanner)
	{
		return new Loop(parse(scanner));
	}

	private static While parseWhile(Scanner scanner)
	{
		boolean negated = ParseTools.option(scanner, Token.Type.STMT_NEGATE, true);
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		Expr condition = ExprParser.parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		Stmt body = StmtParser.parse(scanner);
		return new While(negated, condition, body);
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
