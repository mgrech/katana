package katana.parser;

import katana.ast.Expr;
import katana.ast.Type;
import katana.ast.expr.*;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Optional;

public class ExprParser
{
	public static Expr parse(Scanner scanner)
	{
		Expr expr = parseInitialExpr(scanner);

		for(;;)
		{
			Token token = scanner.token();

			switch(token.type)
			{
			case PUNCT_LPAREN:
				scanner.advance();
				expr = parseFunctionCall(scanner, expr, Optional.empty());
				ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
				break;

			case PUNCT_LBRACKET:
				scanner.advance();
				Expr index = ExprParser.parse(scanner);
				expr = new ArrayAccess(expr, index);
				ParserTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
				break;

			case PUNCT_DOT:
				scanner.advance();
				String name = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
				expr = new MemberAccess(expr, name);
				break;

			default: return expr;
			}
		}
	}

	private static Expr parseInitialExpr(Scanner scanner)
	{
		if(ParserTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParserTools.consume(scanner).value;
			return new NamedValue(name);
		}

		if(ParserTools.option(scanner, Token.Category.LIT, false))
			return parseLiteral(scanner);

		if(ParserTools.option(scanner, Token.Category.MISC, false))
			return parseMisc(scanner);

		ParserTools.unexpectedToken(scanner);
		throw new AssertionError("unreachable");
	}

	private static Expr parseMisc(Scanner scanner)
	{
		Token token = ParserTools.consume(scanner);

		switch(token.type)
		{
		case MISC_SIZEOF:
			ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
			Type stype = TypeParser.parse(scanner);
			ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			return new Sizeof(stype);

		case MISC_ALIGNOF:
			ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
			Type atype = TypeParser.parse(scanner);
			ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			return new Alignof(atype);

		case MISC_OFFSETOF:
			ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
			String type = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
			ParserTools.expect(scanner, Token.Type.PUNCT_COMMA, true);
			String field = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
			ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			return new Offsetof(type, field);

		case MISC_INTRINSIC:
			return parseIntrinsicCall(scanner);

		case MISC_INLINE:
			ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
			String value = ParserTools.expectAndConsume(scanner, Token.Type.LIT_BOOL).value;
			ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			String name = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
			return parseFunctionCall(scanner, new NamedValue(name), Optional.of(value.equals("true")));

		case MISC_ADDRESSOF:
			ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
			Expr aexpr = ExprParser.parse(scanner);
			ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			return new Addressof(aexpr);

		case MISC_DEREFERENCE:
			ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
			Expr dexpr = ExprParser.parse(scanner);
			ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
			return new Dereference(dexpr);

		default: throw new AssertionError("unreachable");
		}
	}

	private static Expr parseLiteral(Scanner scanner)
	{
		Token token = ParserTools.consume(scanner);

		switch(token.type)
		{
		case LIT_NULL: return new LitNull();
		case LIT_UNINIT: return new LitUninit();

		case LIT_BOOL:
			String bvalue = ParserTools.consume(scanner).value;
			return new LitBool(bvalue.equals("true"));

		case LIT_INT:
			String ivalue = ParserTools.consume(scanner).value;
			return new LitInt(new BigInteger(ivalue));

		case LIT_FLOAT:
			String fvalue = ParserTools.consume(scanner).value;
			return new LitFloat(new BigDecimal(fvalue));

		case LIT_STRING:
			String svalue = ParserTools.consume(scanner).value;
			return new LitString(svalue);

		default: throw new AssertionError("unreachable");
		}
	}

	private static IntrinsicCall parseIntrinsicCall(Scanner scanner)
	{
		String name = ParserTools.expectAndConsume(scanner, Token.Type.IDENT).value;
		ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		ArrayList<Expr> args = parseArguments(scanner);
		ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		return new IntrinsicCall(name, args);
	}

	private static FunctionCall parseFunctionCall(Scanner scanner, Expr expr, Optional<Boolean> inline)
	{
		ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		ArrayList<Expr> args = parseArguments(scanner);
		ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		return new FunctionCall(expr, args, inline);
	}

	private static ArrayList<Expr> parseArguments(Scanner scanner)
	{
		ArrayList<Expr> args = new ArrayList<>();

		if(ParserTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
			return args;

		args.add(ExprParser.parse(scanner));

		while(ParserTools.option(scanner, Token.Type.PUNCT_COMMA, true))
			args.add(ExprParser.parse(scanner));

		return args;
	}
}
