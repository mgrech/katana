package katana.parser;

import katana.ast.Path;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ParseTools
{
	public static Path path(Scanner scanner)
	{
		Supplier<String> parseComponent = () -> consumeExpected(scanner, Token.Type.IDENT).value;
		ArrayList<String> components = separated(scanner, Token.Type.PUNCT_DOT, parseComponent);
		return new Path(components);
	}

	public static <T> ArrayList<T> separated(Scanner scanner, Token.Type separator, Supplier<T> parser)
	{
		ArrayList<T> result = new ArrayList<>();

		do result.add(parser.get());
		while(option(scanner, separator, true));

		return result;
	}

	public static <T> T parenthesized(Scanner scanner, Supplier<T> func)
	{
		ParseTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		T result = func.get();
		ParseTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		return result;
	}

	public static boolean option(Scanner scanner, Predicate<Token> predicate, boolean eat)
	{
		if(!predicate.test(scanner.token()))
			return false;

		if(eat)
			scanner.advance();

		return true;
	}

	public static boolean option(Scanner scanner, Token.Category category, boolean eat)
	{
		return option(scanner, (t) -> t.category == category, eat);
	}

	public static boolean option(Scanner scanner, Token.Type type, boolean eat)
	{
		return option(scanner, (t) -> t.type == type, eat);
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

	public static Token consumeExpected(Scanner scanner, Token.Category category)
	{
		expect(scanner, category, false);
		return consume(scanner);
	}

	public static Token consumeExpected(Scanner scanner, Token.Type type)
	{
		expect(scanner, type, false);
		return consume(scanner);
	}

	public static <T> void unexpectedToken(Scanner scanner, T expected)
	{
		throw new RuntimeException(String.format("unexpected token %s, expected %s", scanner.token(), expected));
	}

	public static void unexpectedToken(Scanner scanner)
	{
		throw new RuntimeException("unexpected token " + scanner.token());
	}
}
