package katana.backend.llvm;

import katana.platform.Os;
import katana.platform.TargetTriple;
import katana.project.Project;
import katana.project.ProjectType;
import katana.utils.Maybe;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BuildRunner
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

	private static Maybe<Path> compileCFile(Path path, TargetTriple target, ProjectType type) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		command.add("-undef");
		command.add("-DKATANA_ARCH_" + target.arch.name());
		command.add("-DKATANA_OS_" + target.os.name());

		command.add("-c");
		command.add("-nostdinc");
		command.add("-ffreestanding");
		command.add("-fno-strict-aliasing");

		switch(type)
		{
		case EXECUTABLE:
			command.add("-fPIE");
			break;

		case LIBRARY:
			command.add("-fPIC");
			command.add("-fvisibility=hidden");
			break;

		default: throw new AssertionError("unreachable");
		}

		if(target.os == Os.WINDOWS)
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

	private static Maybe<Path> compileLlvmFile(Project project, Path path) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");
		command.add("-c");
		command.add("-Wno-override-module");

		switch(project.type)
		{
		case EXECUTABLE:
			command.add("-fPIE");
			break;

		case LIBRARY:
			command.add("-fPIC");
			break;

		default: throw new AssertionError("unreachable");
		}

		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + ".o";
		command.add(filename);

		return runCommand(command) ? Maybe.some(Paths.get(filename)) : Maybe.none();
	}

	private static String fileExtensionFor(ProjectType type, TargetTriple target)
	{
		switch(type)
		{
		case EXECUTABLE:
			switch(target.os)
			{
			case WINDOWS: return ".exe";
			default:      return "";
			}

		case LIBRARY:
			switch(target.os)
			{
			case WINDOWS: return ".dll";
			case MACOS:   return ".dylib";
			case LINUX:   return ".so";
			default: throw new AssertionError("unreachable");
			}

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private static boolean link(Project project, List<Path> filePaths, TargetTriple target) throws IOException
	{
		String binaryName = project.name + fileExtensionFor(project.type, target);

		List<String> command = new ArrayList<>();

		if(target.os == Os.WINDOWS)
		{
			command.add("lld-link");

			switch(project.type)
			{
			case EXECUTABLE:
				command.add("/subsystem:console");
				command.add("/entry:_Katana_main");
				break;

			case LIBRARY:
				command.add("/dll");
				command.add("/entry:_Katana_DllMain");
				break;

			default: throw new AssertionError("unreachable");
			}

			command.add("/nodefaultlib");
			command.add("/out:" + binaryName);
		}

		else
		{
			command.add("ld");

			switch(project.type)
			{
			case EXECUTABLE:
				command.add("-e_Katana_main");
				break;

			case LIBRARY:
				command.add("-shared");
				break;

			default: throw new AssertionError("unreachable");
			}

			command.add("-nostdlib");
			command.add("-rpath");
			command.add("$ORIGIN");

			command.add("-o");
			command.add(binaryName);
		}

		// append all source files
		filePaths.stream().map(Path::toString).forEach(command::add);

		return runCommand(command);
	}

	public static void build(Project project, TargetTriple target) throws IOException
	{
		List<Path> objectFiles = new ArrayList<>();

		Path katanaOutput = Paths.get(project.name + ".ll").toAbsolutePath().normalize();

		Maybe<Path> katanaOutputObjectFile = compileLlvmFile(project, katanaOutput);

		if(katanaOutputObjectFile.isNone())
			return;

		objectFiles.add(katanaOutputObjectFile.unwrap());

		for(Path path : project.cFiles)
		{
			Maybe<Path> objectFile = compileCFile(path, target, project.type);

			if(objectFile.isNone())
				return;

			objectFiles.add(objectFile.unwrap());
		}

		if(link(project, objectFiles, target))
			System.out.println("build successful.");
	}
}
