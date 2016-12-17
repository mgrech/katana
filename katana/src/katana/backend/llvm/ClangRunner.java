package katana.backend.llvm;

import katana.platform.Os;
import katana.platform.TargetTriple;
import katana.project.Project;
import katana.utils.Maybe;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ClangRunner
{
	private static boolean runCommand(List<String> command) throws IOException
	{
		System.out.println(String.join(" ", command));

		ProcessBuilder builder = new ProcessBuilder(command);
		builder.inheritIO();

		Process process = builder.start();

		try
		{
			return process.waitFor() == 0;
		}

		catch(InterruptedException ex)
		{}

		throw new AssertionError("unreachable");
	}

	private static Maybe<Path> compileCFile(Path path, TargetTriple triple) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		command.add("-undef");
		command.add("-DKATANA_ARCH_" + triple.arch.name());
		command.add("-DKATANA_OS_" + triple.os.name());

		command.add("-c");
		command.add("-nostdinc");
		command.add("-ffreestanding");
		command.add("-fno-strict-aliasing");

		if(triple.os == Os.WINDOWS)
			command.add("-fno-ms-extensions");

		command.add("-std=c11");
		command.add("-pedantic");
		command.add("-Wall");

		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + ".o";
		command.add(filename);

		return runCommand(command) ? Maybe.some(Paths.get(filename)) : Maybe.none();
	}

	private static Maybe<Path> compileLlvmFile(Path path) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");
		command.add("-c");
		command.add("-Wno-override-module");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + ".o";
		command.add(filename);

		return runCommand(command) ? Maybe.some(Paths.get(filename)) : Maybe.none();
	}

	private static boolean link(List<Path> filePaths, TargetTriple triple) throws IOException
	{
		List<String> command = new ArrayList<>();

		if(triple.os == Os.WINDOWS)
		{
			command.add("lld-link");
			command.add("/entry:main");
			command.add("/nodefaultlib");
			command.add("/out:program.exe");
		}

		else
		{
			command.add("ld");
			command.add("-o");
			command.add("program");
			command.add("-nostdlib");
			command.add("-rpath");
			command.add("$ORIGIN");
			command.add("-emain");
		}

		for(Path filePath : filePaths)
			command.add(filePath.toString());

		return runCommand(command);
	}

	public static void build(Project project, TargetTriple triple) throws IOException
	{
		List<Path> objectFiles = new ArrayList<>();

		Path programll = Paths.get("program.ll").toAbsolutePath().normalize();

		Maybe<Path> programllObjectFile = compileLlvmFile(programll);

		if(programllObjectFile.isNone())
			return;

		objectFiles.add(programllObjectFile.unwrap());

		for(Path path : project.cFiles)
		{
			Maybe<Path> objectFile = compileCFile(path, triple);

			if(objectFile.isNone())
				return;

			objectFiles.add(objectFile.unwrap());
		}

		if(link(objectFiles, triple))
			System.out.println("build successful.");
	}
}
