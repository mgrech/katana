package katana.compiler;

import katana.ast.Decl;
import katana.ast.File;
import katana.parser.FileParser;
import katana.sema.*;
import katana.sema.FileDeclVisitor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class Main
{
	public static ArrayList<Path> discoverSourceFiles(Path root) throws IOException
	{
		ArrayList<Path> paths = new ArrayList<>();

		Files.walkFileTree(root, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
			{
				if(attrs.isRegularFile() && path.toString().endsWith(".kat"))
					paths.add(path);

				return FileVisitResult.CONTINUE;
			}
		});

		return paths;
	}

	public static void main(String[] args) throws IOException
	{
		Program program = new Program();

		ArrayList<Path> paths = discoverSourceFiles(Paths.get("."));

		for(Path path : paths)
		{
			File file = FileParser.parse(path);
			FileDeclVisitor visitor = new FileDeclVisitor(program);

			for(Decl decl : file.decls)
				decl.accept(visitor);

			// todo: verify imports
		}
	}
}
