package katana.parser;

import katana.scanner.Scanner;
import katana.scanner.Token;

public class ParserTools
{
	public static boolean option(Scanner scanner, Token.Category category, boolean eat)
	{
		if(scanner.token().category != category)
			return false;

		if(eat)
			scanner.advance();

		return true;
	}

	public static boolean option(Scanner scanner, Token.Type type, boolean eat)
	{
		if(scanner.token().type != type)
			return false;

		if(eat)
			scanner.advance();

		return true;
	}

	public static void expect(Scanner scanner, Token.Category category, boolean eat)
	{
		if(!option(scanner, category, eat))
			unexpectedToken(scanner, category);
	}

	public static void expect(Scanner scanner, Token.Type type, boolean eat)
	{
		if(!option(scanner, type, eat))
			unexpectedToken(scanner, type);
	}

	public static Token consume(Scanner scanner)
	{
		Token token = scanner.token();
		scanner.advance();
		return token;
	}

	public static Token expectAndConsume(Scanner scanner, Token.Category category)
	{
		expect(scanner, category, false);
		return consume(scanner);
	}

	public static Token expectAndConsume(Scanner scanner, Token.Type type)
	{
		expect(scanner, type, false);
		return consume(scanner);
	}

	public static void unexpectedToken(Scanner scanner, Token.Type expected)
	{
		throw new RuntimeException("unexpected token " + scanner.token() + ", expected " + expected);
	}

	public static void unexpectedToken(Scanner scanner, Token.Category expected)
	{
		throw new RuntimeException("unexpected token " + scanner.token() + ", expected " + expected);
	}

	public static void unexpectedToken(Scanner scanner)
	{
		throw new RuntimeException("unexpected token " + scanner.token());
	}
}
