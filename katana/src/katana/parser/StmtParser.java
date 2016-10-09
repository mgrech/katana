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

import katana.ast.expr.AstExpr;
import katana.ast.stmt.*;
import katana.ast.type.AstType;
import katana.scanner.Scanner;
import katana.scanner.ScannerState;
import katana.scanner.Token;
import katana.utils.Maybe;

public class StmtParser
{
	public static AstStmt parse(Scanner scanner)
	{
		if(ParseTools.option(scanner, Token.Type.STMT_LOCAL, true))
			return parseLocal(scanner);

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

	private static AstStmt parseLocal(Scanner scanner)
	{
		ScannerState state = scanner.capture();

		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParseTools.consume(scanner).value;

			if(ParseTools.option(scanner, Token.Type.PUNCT_ASSIGN, true))
			{
				AstExpr init = ExprParser.parse(scanner);
				ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
				return new AstStmtLocal(Maybe.none(), name, init);
			}
		}

		scanner.backtrack(state);

		AstType type = TypeParser.parse(scanner);
		String name = ParseTools.consumeExpected(scanner, Token.Type.IDENT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_ASSIGN, true);
		AstExpr init = ExprParser.parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstStmtLocal(Maybe.some(type), name, init);
	}

	private static AstStmt parseIf(Scanner scanner)
	{
		boolean negated = ParseTools.option(scanner, Token.Type.STMT_NEGATE, true);
		AstExpr condition = ParseTools.parenthesized(scanner, () -> ExprParser.parse(scanner));
		AstStmt then = parse(scanner);

		Maybe<AstStmt> else_ = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.STMT_ELSE, true))
			else_ = Maybe.some(parse(scanner));

		if(else_.isNone())
			return new AstStmtIf(negated, condition, then);

		return new AstStmtIfElse(negated, condition, then, else_.unwrap());
	}

	private static AstStmtGoto parseGoto(Scanner scanner)
	{
		String label = ParseTools.consumeExpected(scanner, Token.Type.STMT_LABEL).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstStmtGoto(label);
	}

	private static AstStmtReturn parseReturn(Scanner scanner)
	{
		if(ParseTools.option(scanner, Token.Type.PUNCT_SCOLON, true))
			return new AstStmtReturn(Maybe.none());

		AstExpr expr = ExprParser.parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstStmtReturn(Maybe.some(expr));
	}

	private static AstStmtLoop parseLoop(Scanner scanner)
	{
		return new AstStmtLoop(parse(scanner));
	}

	private static AstStmtWhile parseWhile(Scanner scanner)
	{
		boolean negated = ParseTools.option(scanner, Token.Type.STMT_NEGATE, true);
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		AstExpr condition = ExprParser.parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		AstStmt body = StmtParser.parse(scanner);
		return new AstStmtWhile(negated, condition, body);
	}

	private static AstStmtLabel parseLabel(Scanner scanner)
	{
		String label = ParseTools.consume(scanner).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_COLON, true);
		return new AstStmtLabel(label);
	}

	private static AstStmtCompound parseCompound(Scanner scanner)
	{
		AstStmtCompound comp = new AstStmtCompound();

		while(!ParseTools.option(scanner, Token.Type.PUNCT_RBRACE, true))
			comp.body.add(parse(scanner));

		return comp;
	}

	private static AstStmtExprStmt parseExprStmt(Scanner scanner)
	{
		AstExpr expr = ExprParser.parse(scanner);
		ParseTools.expect(scanner, Token.Type.PUNCT_SCOLON, true);
		return new AstStmtExprStmt(expr);
	}
}
