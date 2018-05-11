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

import io.katana.compiler.ast.expr.AstExpr;
import io.katana.compiler.ast.stmt.*;
import io.katana.compiler.ast.type.AstType;
import io.katana.compiler.scanner.TokenType;
import io.katana.compiler.utils.Maybe;

public class StmtParser
{
	public static AstStmt parse(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.STMT_LOCAL, true))
			return parseLocal(ctx);

		if(ParseTools.option(ctx, TokenType.STMT_IF, true))
			return parseIf(ctx, false);

		if(ParseTools.option(ctx, TokenType.STMT_UNLESS, true))
			return parseIf(ctx, true);

		if(ParseTools.option(ctx, TokenType.STMT_GOTO, true))
			return parseGoto(ctx);

		if(ParseTools.option(ctx, TokenType.STMT_RETURN, true))
			return parseReturn(ctx);

		if(ParseTools.option(ctx, TokenType.STMT_LOOP, true))
			return parseLoop(ctx);

		if(ParseTools.option(ctx, TokenType.STMT_WHILE, true))
			return parseWhile(ctx, false);

		if(ParseTools.option(ctx, TokenType.STMT_UNTIL, true))
			return parseWhile(ctx, true);

		if(ParseTools.option(ctx, TokenType.STMT_LABEL, false))
			return parseLabel(ctx);

		if(ParseTools.option(ctx, TokenType.PUNCT_LBRACE, true))
			return parseCompound(ctx);

		return parseExprStmt(ctx);
	}

	private static Maybe<AstExpr> parseLocalInitAndScolon(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.MISC_UNDEF, true))
		{
			ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
			return Maybe.none();
		}

		AstExpr init = ExprParser.parse(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return Maybe.some(init);
	}

	private static AstStmt parseLocal(ParseContext ctx)
	{
		ParseContext tmp = ctx.clone();

		if(ParseTools.option(ctx, TokenType.IDENT, false))
		{
			String name = (String)ParseTools.consume(ctx).value;

			if(ParseTools.option(ctx, "=", true))
			{
				Maybe<AstExpr> init = parseLocalInitAndScolon(ctx);
				return new AstStmtLocal(Maybe.none(), name, init);
			}
		}

		ctx.backtrack(tmp);

		AstType type = TypeParser.parse(ctx);
		String name = (String)ParseTools.consumeExpected(ctx, TokenType.IDENT).value;
		ParseTools.expect(ctx, "=", true);

		Maybe<AstExpr> init = parseLocalInitAndScolon(ctx);
		return new AstStmtLocal(Maybe.some(type), name, init);
	}

	private static AstStmt parseIf(ParseContext ctx, boolean negated)
	{
		AstExpr condition = ParseTools.parenthesized(ctx, () -> ExprParser.parse(ctx));
		AstStmt then = parse(ctx);

		Maybe<AstStmt> else_ = Maybe.none();

		if(ParseTools.option(ctx, TokenType.STMT_ELSE, true))
			else_ = Maybe.some(parse(ctx));

		if(else_.isNone())
			return new AstStmtIf(negated, condition, then);

		return new AstStmtIfElse(negated, condition, then, else_.unwrap());
	}

	private static AstStmtGoto parseGoto(ParseContext ctx)
	{
		String label = (String)ParseTools.consumeExpected(ctx, TokenType.STMT_LABEL).value;
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstStmtGoto(label);
	}

	private static AstStmtReturn parseReturn(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.PUNCT_SCOLON, true))
			return new AstStmtReturn(Maybe.none());

		AstExpr expr = ExprParser.parse(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstStmtReturn(Maybe.some(expr));
	}

	private static AstStmtLoop parseLoop(ParseContext ctx)
	{
		return new AstStmtLoop(parse(ctx));
	}

	private static AstStmtWhile parseWhile(ParseContext ctx, boolean negated)
	{
		ParseTools.expect(ctx, TokenType.PUNCT_LPAREN, true);
		AstExpr condition = ExprParser.parse(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_RPAREN, true);
		AstStmt body = StmtParser.parse(ctx);
		return new AstStmtWhile(negated, condition, body);
	}

	private static AstStmtLabel parseLabel(ParseContext ctx)
	{
		String label = (String)ParseTools.consume(ctx).value;
		ParseTools.expect(ctx, ":", true);
		return new AstStmtLabel(label);
	}

	private static AstStmtCompound parseCompound(ParseContext ctx)
	{
		AstStmtCompound comp = new AstStmtCompound();

		while(!ParseTools.option(ctx, TokenType.PUNCT_RBRACE, true))
			comp.body.add(parse(ctx));

		return comp;
	}

	private static AstStmtExprStmt parseExprStmt(ParseContext ctx)
	{
		AstExpr expr = ExprParser.parse(ctx);
		ParseTools.expect(ctx, TokenType.PUNCT_SCOLON, true);
		return new AstStmtExprStmt(expr);
	}
}
