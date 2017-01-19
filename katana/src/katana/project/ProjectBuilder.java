// Copyright 2016-2017 Markus Grech
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package katana.project;

import katana.Katana;
import katana.diag.CompileException;
import katana.platform.Os;
import katana.platform.TargetTriple;
import katana.project.conditionals.Conditional;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProjectBuilder
{
	private static ProcessBuilder buildCommand(List<String> command, TargetTriple target)
	{
		if(target.os == Os.WINDOWS)
			return VsTools.buildDevCmdCommand(command, target);

		return new ProcessBuilder(command);
	}

	private static void runCommand(List<String> command, TargetTriple target)
	{
		System.out.println(String.join(" ", command));

		ProcessBuilder builder = buildCommand(command, target);
		builder.inheritIO();

		try
		{
			Process process = builder.start();
			int exitCode = process.waitFor();

			if(exitCode != 0)
				throw new CompileException(String.format("process '%s' exited with code %s.", command.get(0), exitCode));
		}

		catch(IOException | InterruptedException ex)
		{
			throw new RuntimeException(ex);
		}
	}

	private static void addPpCompileFlags(List<String> command, TargetTriple target)
	{
		command.add("-DKATANA_ARCH_" + target.arch.name());
		command.add("-DKATANA_OS_" + target.os.name());
	}

	private static void addPicFlag(List<String> command, ProjectType type)
	{
		switch(type)
		{
		case EXECUTABLE:
			command.add("-fPIE");
			break;

		case LIBRARY:
			command.add("-fPIC");
			break;

		default: throw new AssertionError("unreachable");
		}
	}

	private static final Path KATANA_INCDIR = Katana.HOME.resolve("include").toAbsolutePath().normalize();

	private static String objectFileExtension(TargetTriple target)
	{
		if(target.os == Os.WINDOWS)
			return ".obj";

		return ".o";
	}

	private static void addCommonLangCompileFlags(List<String> command, TargetTriple target)
	{
		command.add("-pedantic");
		command.add("-Wall");
		command.add("-fno-strict-aliasing");
		command.add("-fvisibility=hidden");

		command.add("-I" + KATANA_INCDIR);

		if(target.os == Os.WINDOWS)
			command.add("-fms-compatibility-version=19");
	}

	private static Path compileAsmFile(Path path, TargetTriple target, ProjectType type) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		addPicFlag(command, type);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runCommand(command, target);
		return Paths.get(filename);
	}

	private static Path compileCFile(Path path, TargetTriple target, ProjectType type) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		command.add("-std=c11");
		addPpCompileFlags(command, target);
		addCommonLangCompileFlags(command, target);
		addPicFlag(command, type);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runCommand(command, target);
		return Paths.get(filename);
	}

	private static Path compileCppFile(Path path, TargetTriple target, ProjectType type) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		command.add("-std=c++14");
		addPpCompileFlags(command, target);
		addCommonLangCompileFlags(command, target);
		addPicFlag(command, type);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runCommand(command, target);
		return Paths.get(filename);
	}

	private static Path compileLlvmFile(Project project, TargetTriple target, Path path) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		command.add("-Wno-override-module");
		addPicFlag(command, project.type);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runCommand(command, target);
		return Paths.get(filename);
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

	private static final Path LIBDIR = Katana.HOME.resolve("lib").toAbsolutePath().normalize();

	private static void link(Project project, List<Path> filePaths, TargetTriple target) throws IOException
	{
		String binaryName = project.name + fileExtensionFor(project.type, target);

		List<String> command = new ArrayList<>();

		if(target.os == Os.WINDOWS)
		{
			command.add("link");
			command.add("/nologo");

			if(project.type == ProjectType.EXECUTABLE)
				command.add("/subsystem:console");

			if(project.type == ProjectType.LIBRARY)
				command.add("/dll");

			command.add("/libpath:" + LIBDIR);

			for(String lib : project.libs)
				command.add(lib + ".lib");

			// https://blogs.msdn.microsoft.com/vcblog/2015/03/03/introducing-the-universal-crt/
			command.add("msvcrt.lib");
			command.add("vcruntime.lib");
			command.add("ucrt.lib");

			command.add("/out:" + binaryName);
		}

		else
		{
			command.add("clang");

			if(project.type == ProjectType.LIBRARY)
				command.add("-shared");

			command.add("-Wl,-rpath,$ORIGIN");
			command.add("-Wl,-z,now");
			command.add("-Wl,-z,relro");

			command.add("-L" + LIBDIR);

			for(String lib : project.libs)
				command.add("-l" + lib);

			command.add("-o");
			command.add(binaryName);
		}

		// append all source files
		filePaths.stream().map(Path::toString).forEach(command::add);
		runCommand(command, target);
	}

	public static void build(Project project, TargetTriple target) throws IOException
	{
		List<Path> objectFiles = new ArrayList<>();

		Path katanaOutput = Paths.get(project.name + ".ll").toAbsolutePath().normalize();

		objectFiles.add(compileLlvmFile(project, target, katanaOutput));

		for(Conditional<Path> cpath : project.externFiles)
		{
			Path path = cpath.get(target);

			if(path == null)
				continue;

			String pathString = path.toString();

			if(pathString.endsWith(Katana.FILE_EXTENSION_ASM))
				objectFiles.add(compileAsmFile(path, target, project.type));

			else if(pathString.endsWith(Katana.FILE_EXTENSION_C))
				objectFiles.add(compileCFile(path, target, project.type));

			else if(pathString.endsWith(Katana.FILE_EXTENSION_CPP))
				objectFiles.add(compileCppFile(path, target, project.type));

			else
				throw new AssertionError("unreachable");
		}

		link(project, objectFiles, target);
		System.out.println("build successful.");
	}
}
