package katana.parser;

import katana.ast.Decl;
import katana.ast.File;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class FileParser
{
	public static File parse(Path path) throws IOException
	{
		byte[] data = Files.readAllBytes(path);
		int[] codepoints = new String(data, StandardCharsets.UTF_8).codePoints().toArray();

		Scanner scanner = new Scanner(codepoints);
		scanner.advance();

		ArrayList<Decl> decls = new ArrayList<>();

		while(scanner.token().type != Token.Type.END)
			decls.add(DeclParser.parse(scanner));

		return new File(path, decls);
	}
}
