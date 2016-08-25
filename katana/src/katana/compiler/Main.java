package katana.compiler;

import katana.ast.Decl;
import katana.ast.File;
import katana.backend.PlatformContext;
import katana.backend.llvm.ProgramCodeGen;
import katana.backend.llvm.x86_64.PlatformContextLlvmX86;
import katana.parser.FileParser;
import katana.sema.FileValidator;
import katana.sema.Program;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
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
			if(program.findModule(path).isNone())
				throw new RuntimeException("import of unknown module '" + path + "'");
	}

	public static void buildCommand() throws IOException
	{
		PlatformContext context = new PlatformContextLlvmX86();
		Program program = new Program();

		ArrayList<Path> paths = discoverSourceFiles(Paths.get("./source"));
		Set<katana.ast.Path> imports = new HashSet<>();

		for(Path path : paths)
		{
			File file = FileParser.parse(path);
			FileValidator visitor = new FileValidator(context, program);

			for(Decl decl : file.decls)
				decl.accept(visitor);

			imports.addAll(visitor.imports());
		}

		validateImports(program, imports);

		String output = ProgramCodeGen.generate(program, context);

		try(OutputStream stream = new FileOutputStream("output/program.ll"))
		{
			stream.write(output.getBytes(StandardCharsets.UTF_8));
		}
	}

	public static void initCommand() throws IOException
	{
		Files.createDirectories(Paths.get("./source"));
		Files.createDirectories(Paths.get("./output"));
	}

	public static void main(String[] args) throws IOException
	{
		if(args.length != 1)
		{
			System.err.println("invalid number of arguments");
			System.exit(1);
		}

		try
		{
			switch(args[0])
			{
			case "init":
				initCommand();
				return;

			case "build":
				buildCommand();
				break;

			default:
				System.err.println("invalid command");
				System.exit(1);
			}
		}

		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
