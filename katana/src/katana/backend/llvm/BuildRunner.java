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

package katana.backend.llvm;

import katana.Katana;
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

	private static final Path INCDIR = Katana.HOME.resolve("include").toAbsolutePath().normalize();

	private static void addCommonLangCompileFlags(List<String> command, TargetTriple target)
	{
		command.add("-pedantic");
		command.add("-Wall");
		command.add("-fno-strict-aliasing");
		command.add("-fvisibility=hidden");

		command.add("-I" + INCDIR);
	}

	private static Maybe<Path> compileAsmFile(Path path, ProjectType type) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		addPicFlag(command, type);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + ".o";
		command.add(filename);

		return runCommand(command) ? Maybe.some(Paths.get(filename)) : Maybe.none();
	}

	private static Maybe<Path> compileCFile(Path path, TargetTriple target, ProjectType type) throws IOException
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

		String filename = path.getFileName() + ".o";
		command.add(filename);

		return runCommand(command) ? Maybe.some(Paths.get(filename)) : Maybe.none();
	}

	private static Maybe<Path> compileCppFile(Path path, TargetTriple target, ProjectType type) throws IOException
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

		String filename = path.getFileName() + ".o";
		command.add(filename);

		return runCommand(command) ? Maybe.some(Paths.get(filename)) : Maybe.none();
	}

	private static Maybe<Path> compileLlvmFile(Project project, Path path) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		command.add("-Wno-override-module");
		addPicFlag(command, project.type);

		command.add("-c");
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

	private static final Path LIBDIR = Katana.HOME.resolve("lib").toAbsolutePath().normalize();
	private static final String RTLIB = "krt";

	private static boolean link(Project project, List<Path> filePaths, TargetTriple target) throws IOException
	{
		String binaryName = project.name + fileExtensionFor(project.type, target);

		List<String> command = new ArrayList<>();

		if(target.os == Os.WINDOWS)
		{
			command.add("lld-link");

			if(project.type == ProjectType.EXECUTABLE)
				command.add("/subsystem:console");

			if(project.type == ProjectType.LIBRARY)
				command.add("/dll");

			command.add("/libpath:" + LIBDIR);
			command.add(RTLIB + ".dll");

			command.add("/out:" + binaryName);
		}

		else
		{
			command.add("ld");

			if(project.type == ProjectType.LIBRARY)
				command.add("-shared");

			command.add("-rpath");
			command.add("$ORIGIN");

			command.add("-z");
			command.add("now");
			command.add("-z");
			command.add("relro");

			command.add("-L" + LIBDIR);
			command.add("-l" + RTLIB);

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

		for(Path path : project.asmFiles)
		{
			Maybe<Path> objectFile = compileAsmFile(path, project.type);

			if(objectFile.isNone())
				return;

			objectFiles.add(objectFile.unwrap());
		}

		for(Path path : project.cFiles)
		{
			Maybe<Path> objectFile = compileCFile(path, target, project.type);

			if(objectFile.isNone())
				return;

			objectFiles.add(objectFile.unwrap());
		}

		for(Path path : project.cppFiles)
		{
			Maybe<Path> objectFile = compileCppFile(path, target, project.type);

			if(objectFile.isNone())
				return;

			objectFiles.add(objectFile.unwrap());
		}

		if(link(project, objectFiles, target))
			System.out.println("build successful.");
	}
}
