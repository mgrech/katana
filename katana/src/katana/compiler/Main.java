package katana.compiler;

import katana.scanner.Scanner;
import katana.scanner.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main
{
	public static void main(String[] args) throws IOException
	{
		byte[] data = Files.readAllBytes(Paths.get(args[0]));
		int[] codepoints = new String(data, StandardCharsets.UTF_8).codePoints().toArray();

		Scanner scanner = new Scanner(codepoints);
		scanner.advance();

		for(Token token = scanner.token(); token.type != Token.Type.END; scanner.advance(), token = scanner.token())
			System.out.println(String.format("%s is on line %s, column %s, offset %s", token, scanner.line(), scanner.column(), scanner.offset()));
	}
}
