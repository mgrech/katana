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
import io.katana.compiler.analysis.ProgramValidator;
import io.katana.compiler.ast.AstProgram;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.backend.ResourceGenerator;
import io.katana.compiler.backend.llvm.ProgramCodeGenerator;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.DiagnosticsManager;
import io.katana.compiler.parser.ProgramParser;
import io.katana.compiler.platform.Os;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.scanner.SourceManager;
import io.katana.compiler.sema.SemaProgram;
import io.katana.compiler.utils.Maybe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectBuilder
{
	private static final Path KATANA_INCLUDE_DIR = Katana.HOME.resolve("include");
	private static final Path KATANA_LIBRARY_DIR = Katana.HOME.resolve("lib");

	private static void runBuildCommand(BuildTarget build, List<String> command)
	{
		System.out.println(String.format("[%s] %s", build.name, String.join(" ", command)));

		ProcessBuilder builder = new ProcessBuilder(command);
		builder.inheritIO();
		builder.directory(build.outputDirectory.toFile());

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

	private static void addDefaultDefines(List<String> command, TargetTriple target)
	{
		command.add("-DKATANA_ARCH_" + target.arch.name());
		command.add("-DKATANA_OS_" + target.os.name());
	}

	private static String objectFileExtension(TargetTriple target)
	{
		if(target.os == Os.WINDOWS)
			return ".obj";

		return ".o";
	}

	private static void addDefaultIncludes(List<String> command)
	{
		command.add("-I" + KATANA_INCLUDE_DIR);
	}

	private static Path compileAsmFile(BuildTarget build, Path path, TargetTriple target) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");
		command.addAll(build.asmOptions);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runBuildCommand(build, command);
		return Paths.get(filename);
	}

	private static Path compileCFile(BuildTarget build, Path path, TargetTriple target) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");
		command.addAll(build.cOptions);

		addDefaultDefines(command, target);
		addDefaultIncludes(command);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runBuildCommand(build, command);
		return Paths.get(filename);
	}

	private static Path compileCppFile(BuildTarget build, Path path, TargetTriple target) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang++");
		command.addAll(build.cppOptions);

		addDefaultDefines(command, target);
		addDefaultIncludes(command);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runBuildCommand(build, command);
		return Paths.get(filename);
	}

	private static Path compileLlvmFile(BuildTarget build, TargetTriple target, Path path) throws IOException
	{
		List<String> command = new ArrayList<>();
		command.add("clang");

		command.add("-Wno-override-module");
		command.addAll(build.llvmOptions);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		String filename = path.getFileName() + objectFileExtension(target);
		command.add(filename);

		runBuildCommand(build, command);
		return Paths.get(filename);
	}

	private static String fileExtensionFor(BuildType type, TargetTriple target)
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

	private static void link(BuildTarget build, List<Path> filePaths, TargetTriple target) throws IOException
	{
		String binaryName = build.name + fileExtensionFor(build.type, target);

		List<String> command = new ArrayList<>();

		if(build.sourceFiles.get(FileType.CPP) == null)
			command.add("clang");
		else
			command.add("clang++");

		command.add("-fuse-ld=lld");
		command.addAll(build.linkOptions);

		if(build.type == BuildType.LIBRARY)
			command.add("-shared");

		command.add("-L" + KATANA_LIBRARY_DIR);

		for(String lib : build.systemLibraries)
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

		runBuildCommand(build, command);
	}

	private static Path compileFile(BuildTarget build, FileType fileType, Path path, TargetTriple target) throws IOException
	{
		switch(fileType)
		{
		case ASM: return compileAsmFile(build, path, target);
		case C:   return compileCFile(build, path, target);
		case CPP: return compileCppFile(build, path, target);

		default:
			throw new AssertionError("unreachable");
		}
	}

	private static Maybe<Path> compileKatanaSources(DiagnosticsManager diag, Path root, BuildTarget build, PlatformContext context, Path buildDir) throws IOException
	{
		Set<Path> katanaFiles = build.sourceFiles.get(FileType.KATANA);

		if(katanaFiles == null)
			return Maybe.none();

		SourceManager sourceManager = SourceManager.loadFiles(root, build.sourceFiles.get(FileType.KATANA));
		AstProgram ast = ProgramParser.parse(sourceManager, diag);
		SemaProgram program = ProgramValidator.validate(ast, context);

		if(!diag.successful())
			throw new CompileException(diag.summary());

		Path katanaOutputFile = buildDir.resolve(build.name + ".ll");
		ProgramCodeGenerator.generate(build, program, context, katanaOutputFile);
		return Maybe.some(katanaOutputFile);
	}

	public static void build(DiagnosticsManager diag, Path root, BuildTarget build, PlatformContext context) throws IOException
	{
		if(!build.outputDirectory.toFile().exists())
			Files.createDirectories(build.outputDirectory);

		Maybe<Path> katanaOutput = compileKatanaSources(diag, root, build, context, build.outputDirectory);
		List<Path> objectFiles = new ArrayList<>();

		Path resourcePath = build.outputDirectory.resolve("resources.asm");
		ResourceGenerator.generate(context.target(), build.resourceFiles, resourcePath);

		objectFiles.add(compileAsmFile(build, resourcePath, context.target()));

		if(katanaOutput.isSome())
			objectFiles.add(compileLlvmFile(build, context.target(), katanaOutput.get()));

		for(Map.Entry<FileType, Set<Path>> entry : build.sourceFiles.entrySet())
		{
			FileType type = entry.getKey();
			Set<Path> paths = entry.getValue();

			if(type != FileType.KATANA)
				for(Path path : paths)
					objectFiles.add(compileFile(build, type, path, context.target()));
		}

		link(build, objectFiles, context.target());
		System.out.println(String.format("[%s] build successful.", build.name));
	}
}
