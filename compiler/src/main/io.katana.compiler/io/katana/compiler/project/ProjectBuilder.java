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

package io.katana.compiler.project;

import io.katana.compiler.Katana;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.platform.Os;
import io.katana.compiler.platform.TargetTriple;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectBuilder
{
	private static final Path KATANA_INCLUDE_DIR = Katana.HOME.resolve("include").toAbsolutePath().normalize();
	private static final Path KATANA_LIBRARY_DIR = Katana.HOME.resolve("lib").toAbsolutePath().normalize();

	private static void runCommand(List<String> command)
	{
		System.out.println(String.join(" ", command));

		ProcessBuilder builder = new ProcessBuilder(command);
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

	private static void addPicFlag(List<String> command, TargetTriple triple, ProjectType type)
	{
		// code is always position-independent on windows
		if(triple.os == Os.WINDOWS)
			return;

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

	private static String objectFileExtension(TargetTriple target)
	{
		if(target.os == Os.WINDOWS)
			return ".obj";

		return ".o";
	}

	private static void addCommonLangCompileFlags(List<String> command)
	{
		command.add("-pedantic");
		command.add("-Wall");
		command.add("-fno-strict-aliasing");
		command.add("-fvisibility=hidden");

		command.add("-I" + KATANA_INCLUDE_DIR);
	}

	private static Path compileAsmFile(Path path, TargetTriple target, ProjectType type) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		addPicFlag(command, target, type);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runCommand(command);
		return Paths.get(filename);
	}

	private static Path compileCFile(Path path, TargetTriple target, ProjectType type) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		command.add("-std=c11");
		addPpCompileFlags(command, target);
		addCommonLangCompileFlags(command);
		addPicFlag(command, target, type);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runCommand(command);
		return Paths.get(filename);
	}

	private static Path compileCppFile(Path path, TargetTriple target, ProjectType type) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang++");

		command.add("-std=c++14");
		addPpCompileFlags(command, target);
		addCommonLangCompileFlags(command);
		addPicFlag(command, target, type);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runCommand(command);
		return Paths.get(filename);
	}

	private static Path compileLlvmFile(Project project, TargetTriple target, Path path) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		command.add("-Wno-override-module");
		addPicFlag(command, target, project.type);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runCommand(command);
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

	private static void link(Project project, List<Path> filePaths, TargetTriple target) throws IOException
	{
		String binaryName = project.name + fileExtensionFor(project.type, target);

		List<String> command = new ArrayList<>();

		command.add("clang++");
		command.add("-fuse-ld=lld");

		if(project.type == ProjectType.LIBRARY)
			command.add("-shared");

		command.add("-L" + KATANA_LIBRARY_DIR);

		for(String lib : project.libraries)
			command.add("-l" + lib);

		if(target.os != Os.WINDOWS)
		{
			command.add("-Wl,-rpath,$ORIGIN");
			command.add("-Wl,-z,now");
			command.add("-Wl,-z,relro");
		}

		command.add("-o");
		command.add(binaryName);

		// append all source files
		filePaths.stream().map(Path::toString).forEach(command::add);

		runCommand(command);
	}

	private static Path compileFile(FileType fileType, Path path, TargetTriple target, ProjectType projectType) throws IOException
	{
		switch(fileType)
		{
		case ASM: return compileAsmFile(path, target, projectType);
		case C:   return compileCFile(path, target, projectType);
		case CPP: return compileCppFile(path, target, projectType);

		default:
			throw new AssertionError("unreachable");
		}
	}

	public static void build(Project project, TargetTriple target, Path buildDir) throws IOException
	{
		List<Path> objectFiles = new ArrayList<>();

		Path katanaOutput = buildDir.resolve(project.name + ".ll");
		objectFiles.add(compileLlvmFile(project, target, katanaOutput));

		for(Map.Entry<FileType, Set<Path>> entry : project.sourceFiles.entrySet())
		{
			FileType type = entry.getKey();
			Set<Path> paths = entry.getValue();

			if(type != FileType.KATANA)
				for(Path path : paths)
					objectFiles.add(compileFile(type, path, target, project.type));
		}

		link(project, objectFiles, target);
		System.out.println("build successful.");
	}
}
