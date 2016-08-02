package katana.compiler;

import katana.ast.Decl;
import katana.parser.FileParser;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main
{
	public static void main(String[] args) throws IOException
	{
		byte[] data = Files.readAllBytes(Paths.get(args[0]));
		int[] codepoints = new String(data, StandardCharsets.UTF_8).codePoints().toArray();

		ArrayList<Decl> decls = FileParser.parse(codepoints);

		for(Decl decl : decls)
			System.out.print(decl);
	}
}
