package katana.compiler;

import katana.ast.*;
import katana.ast.decl.Import;
import katana.parser.FileParser;
import katana.sema.FileDeclVisitor;
import katana.sema.Program;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

	private static void validateImports(Program program, Set<katana.ast.Path> imports)
	{
		for(katana.ast.Path path : imports)
			if(!program.findModule(path).isPresent())
				throw new RuntimeException("import of unknown module '" + path + "'");
	}

	public static void main(String[] args) throws IOException
	{
		Program program = new Program();

		ArrayList<Path> paths = discoverSourceFiles(Paths.get("."));
		Set<katana.ast.Path> imports = new HashSet<>();

		for(Path path : paths)
		{
			File file = FileParser.parse(path);
			FileDeclVisitor visitor = new FileDeclVisitor(program);

			for(Decl decl : file.decls)
				decl.accept(visitor);

			imports.addAll(visitor.imports());
		}

		validateImports(program, imports);
	}
}
