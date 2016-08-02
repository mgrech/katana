package katana.parser;

import katana.ast.Decl;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.util.ArrayList;

public class FileParser
{
	public static ArrayList<Decl> parse(int[] source)
	{
		Scanner scanner = new Scanner(source);
		scanner.advance();

		ArrayList<Decl> decls = new ArrayList<>();

		while(scanner.token().type != Token.Type.END)
			decls.add(DeclParser.parse(scanner));

		return decls;
	}
}
