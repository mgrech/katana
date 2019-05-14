// Copyright 2016-2019 Markus Grech
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
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.backend.ResourceGenerator;
import io.katana.compiler.backend.llvm.codegen.ProgramCodegen;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.DiagnosticsManager;
import io.katana.compiler.parser.ProgramParser;
import io.katana.compiler.platform.Os;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.scanner.SourceManager;
import io.katana.compiler.utils.FileUtils;
import io.katana.compiler.utils.Maybe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectBuilder
{
	private static final Path KATANA_INCLUDE_DIR = Katana.HOME.resolve("include");
	private static final Path KATANA_LIBRARY_DIR = Katana.HOME.resolve("lib");
	private static final String BUILD_TMPDIR = "tmp";
	private static final String BUILD_OUTDIR = "out";
	private static final String RESOURCES_FILE = "kt_resources.asm";

	private static void runBuildCommand(Path dir, BuildTarget build, String description, List<String> command)
	{
		System.out.printf("[%s] %s: %s\n", build.name, description, String.join(" ", command));

		var builder = new ProcessBuilder(command);
		builder.inheritIO();
		builder.directory(dir.toFile());

		try
		{
			var process = builder.start();
			var exitCode = process.waitFor();

			if(exitCode != 0)
				throw new CompileException(String.format("[%s] Error: Process '%s' exited with code %s.", build.name, command.get(0), exitCode));
		}
		catch(IOException | InterruptedException ex)
		{
			throw new RuntimeException(ex);
		}
	}

	private static void addDefaultDefines(List<String> command, BuildTarget build, TargetTriple target)
	{
		command.add("-DKATANA_ARCH_" + target.arch.name());
		command.add("-DKATANA_OS_" + target.os.name());
		command.add("-DKATANA_TYPE_" + build.type.name());
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

	private static Path compileAsmFile(Path root, Path buildDir, BuildTarget build, Path path, TargetTriple target)
	{
		var command = new ArrayList<String>();
		command.add("clang");
		command.addAll(build.asmOptions);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		var filename = path.getFileName() + objectFileExtension(target);
		var outputPath = buildDir.resolve(filename);
		command.add(outputPath.toString());

		runBuildCommand(root, build, "Compiling", command);
		return outputPath;
	}

	private static Path compileCFile(Path root, Path buildDir, BuildTarget build, Path path, TargetTriple target)
	{
		var command = new ArrayList<String>();
		command.add("clang");
		command.addAll(build.cOptions);

		addDefaultDefines(command, build, target);
		addDefaultIncludes(command);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		var filename = path.getFileName() + objectFileExtension(target);
		var outputPath = buildDir.resolve(filename);
		command.add(outputPath.toString());

		runBuildCommand(root, build, "Compiling", command);
		return outputPath;
	}

	private static Path compileCppFile(Path root, Path buildDir, BuildTarget build, Path path, TargetTriple target)
	{
		var command = new ArrayList<String>();
		command.add("clang++");
		command.addAll(build.cppOptions);

		addDefaultDefines(command, build, target);
		addDefaultIncludes(command);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		var filename = path.getFileName() + objectFileExtension(target);
		var outputPath = buildDir.resolve(filename);
		command.add(outputPath.toString());

		runBuildCommand(root, build, "Compiling", command);
		return outputPath;
	}

	private static Path compileLlvmFile(Path root, Path buildDir, BuildTarget build, TargetTriple target, Path path)
	{
		var command = new ArrayList<String>();
		command.add("clang");

		command.add("-Wno-override-module");
		command.addAll(build.llvmOptions);

		command.add("-c");
		command.add(path.toString());
		command.add("-o");

		var filename = path.getFileName() + objectFileExtension(target);
		var outputPath = buildDir.resolve(filename);
		command.add(outputPath.toString());

		runBuildCommand(root, build, "Compiling", command);
		return outputPath;
	}

	private static String fileExtensionFor(BuildType type, TargetTriple target)
	{
		return switch(type)
		{
		case EXECUTABLE -> switch(target.os)
			{
			case WINDOWS      -> ".exe";
			case LINUX, MACOS -> "";
			default -> throw new AssertionError("unreachable");
			};

		case LIBRARY_STATIC -> switch(target.os)
			{
			case WINDOWS      -> ".lib";
			case LINUX, MACOS -> ".a";
			default -> throw new AssertionError("unreachable");
			};

		case LIBRARY_SHARED ->
			switch(target.os)
			{
			case WINDOWS -> ".dll";
			case LINUX   -> ".so";
			case MACOS   -> ".dylib";
			default -> throw new AssertionError("unreachable");
			};
		};
	}

	private static void findDependenciesRecursively(BuildTarget build, List<BuildTarget> result)
	{
		for(var dependency : build.dependencies)
		{
			if(!result.contains(dependency))
				result.add(dependency);
		}

		for(var dependency : build.dependencies)
			if(dependency.type == BuildType.LIBRARY_STATIC)
				findDependenciesRecursively(dependency, result);
	}

	private static Path link(Path root, Path buildDir, BuildTarget build, List<Path> filePaths, TargetTriple target)
	{
		var binaryName = build.name + fileExtensionFor(build.type, target);
		var buildPath = buildDir.resolve(binaryName);
		var command = new ArrayList<String>();

		if(build.type == BuildType.LIBRARY_STATIC)
		{
			if(target.os == Os.WINDOWS)
				command.add("llvm-ar");
			else
				command.add("ar");

			command.add("rcs");
		}
		else
		{
			if(build.sourceFiles.get(FileType.CPP) == null)
				command.add("clang");
			else
				command.add("clang++");

			if(target.os != Os.MACOS)
				command.add("-fuse-ld=lld");

			command.addAll(build.linkOptions);

			if(build.type == BuildType.LIBRARY_SHARED)
				command.add("-shared");

			if(KATANA_LIBRARY_DIR.toFile().exists())
				command.add("-L" + KATANA_LIBRARY_DIR);

			var dependencies = new ArrayList<BuildTarget>();
			findDependenciesRecursively(build, dependencies);

			var buildRoot = buildDir.getParent().getParent();

			for(var dependency : dependencies)
				command.add("-L" + buildRoot.resolve(dependency.name).resolve(BUILD_OUTDIR));

			for(var dependency : dependencies)
				command.add("-l" + dependency.name);

			for(var lib : build.systemLibraries)
				command.add("-l" + lib);

			command.add("-o");
		}

		command.add(buildPath.toString());

		// append all source files
		filePaths.stream().map(Path::toString).forEach(command::add);

		runBuildCommand(root, build, "Linking", command);
		return buildPath;
	}

	private static Path compileFile(Path root, Path buildDir, BuildTarget build, FileType fileType, Path path, TargetTriple target)
	{
		return switch(fileType)
		{
		case ASM -> compileAsmFile(root, buildDir, build, path, target);
		case C   -> compileCFile  (root, buildDir, build, path, target);
		case CPP -> compileCppFile(root, buildDir, build, path, target);
		default  -> throw new AssertionError("unreachable");
		};
	}

	private static String formatAsSeconds(long nanos)
	{
		return String.format("%.3f s", nanos / 1000 / 1000 / 1000.f);
	}

	private static Maybe<Path> compileKatanaSources(DiagnosticsManager diag, BuildTarget build, PlatformContext context, Path buildDir, BuildOptions options) throws IOException
	{
		var katanaFiles = build.sourceFiles.get(FileType.KATANA);

		if(katanaFiles == null)
			return Maybe.none();

		var katanaOutputFile = buildDir.resolve(build.name + ".ll");
		System.out.printf("[%s] Compiling Katana sources: %s\n", build.name, katanaOutputFile);

		var startTime = System.nanoTime();

		var sourceManager = SourceManager.loadFiles(build.sourceFiles.get(FileType.KATANA));
		var loadTime = System.nanoTime();

		var ast = ProgramParser.parse(sourceManager, diag);
		var parseTime = System.nanoTime();

		var program = ProgramValidator.validate(ast, context);
		var analysisTime = System.nanoTime();

		if(!diag.successful())
			throw new CompileException(diag.summary());

		var module = ProgramCodegen.generate(build, program, context);
		var codegenTime = System.nanoTime();

		FileUtils.writeFile(module.toString(), katanaOutputFile);
		var writeTime = System.nanoTime();

		if(options.printBuildMetrics)
		{
			System.out.printf("[%s] Katana compile-time breakdown:\n", build.name);
			System.out.printf("[%s] Loading/Scanner: %s\n", build.name, formatAsSeconds(loadTime - startTime));
			System.out.printf("[%s] Parser:          %s\n", build.name, formatAsSeconds(parseTime - loadTime));
			System.out.printf("[%s] Analysis:        %s\n", build.name, formatAsSeconds(analysisTime - parseTime));
			System.out.printf("[%s] Codegen:         %s\n", build.name, formatAsSeconds(codegenTime - analysisTime));
			System.out.printf("[%s] Writing output:  %s\n", build.name, formatAsSeconds(writeTime - codegenTime));
			System.out.printf("[%s] Total:           %s\n", build.name, formatAsSeconds(writeTime - startTime));
		}

		return Maybe.some(katanaOutputFile);
	}

	public static Path buildTarget(DiagnosticsManager diag, Path root, Path buildDir, BuildTarget build, PlatformContext context, BuildOptions options) throws IOException
	{
		var tmpDir = buildDir.resolve(BUILD_TMPDIR);
		var outDir = buildDir.resolve(BUILD_OUTDIR);

		Files.createDirectories(buildDir);
		Files.createDirectories(tmpDir);
		Files.createDirectories(outDir);

		var katanaOutput = compileKatanaSources(diag, build, context, tmpDir, options);
		var objectFiles = new ArrayList<Path>();

		if(!build.resourceFiles.isEmpty())
		{
			var resourcePath = tmpDir.resolve(RESOURCES_FILE);
			System.out.printf("[%s] Generating resource file: %s\n", build.name, resourcePath);
			ResourceGenerator.generate(context.target(), build.resourceFiles, resourcePath);
			objectFiles.add(compileAsmFile(root, tmpDir, build, resourcePath, context.target()));
		}

		if(katanaOutput.isSome())
			objectFiles.add(compileLlvmFile(root, tmpDir, build, context.target(), katanaOutput.get()));

		for(var entry : build.sourceFiles.entrySet())
		{
			var type = entry.getKey();
			var paths = entry.getValue();

			if(type != FileType.KATANA)
				for(var path : paths)
					objectFiles.add(compileFile(root, tmpDir, build, type, path, context.target()));
		}

		var outputPath = link(root, outDir, build, objectFiles, context.target());
		System.out.println(String.format("[%s] Target built successfully.", build.name));
		return outputPath;
	}

	private static void addTargetsRecursively(List<BuildTarget> order, BuildTarget target)
	{
		for(var dependency : target.dependencies)
			addTargetsRecursively(order, dependency);

		order.add(target);
	}

	private static List<BuildTarget> removeDuplicates(List<BuildTarget> targets)
	{
		var result = new ArrayList<BuildTarget>();
		var seen = Collections.newSetFromMap(new IdentityHashMap<BuildTarget, Boolean>());

		for(var target : targets)
		{
			if(seen.contains(target))
				continue;

			seen.add(target);
			result.add(target);
		}

		return result;
	}

	private static List<BuildTarget> determineBuildOrder(List<BuildTarget> targets)
	{
		var order = new ArrayList<BuildTarget>();

		for(var target : targets)
			addTargetsRecursively(order, target);

		return removeDuplicates(order);
	}

	public static void buildTargets(DiagnosticsManager diag, Path root, Path buildDir, List<BuildTarget> targets, PlatformContext context, BuildOptions options) throws IOException
	{
		if(buildDir.startsWith(root))
			buildDir = root.relativize(buildDir);

		var targetsInBuildOrder = determineBuildOrder(targets);
		var targetListing = targetsInBuildOrder.stream().map(t -> t.name).collect(Collectors.joining(", "));

		System.out.printf("[*] Building the following targets in order: %s\n", targetListing);

		for(var target : targetsInBuildOrder)
			ProjectBuilder.buildTarget(diag, root, buildDir.resolve(target.name), target, context, options);

		System.out.println("[*] All targets built successfully.");
	}
}
