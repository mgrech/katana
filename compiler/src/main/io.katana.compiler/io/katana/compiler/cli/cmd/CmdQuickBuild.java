// Copyright 2019 Markus Grech
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

package io.katana.compiler.cli.cmd;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.DiagnosticsManager;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.project.*;
import io.katana.compiler.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Command(name = "quickbuild", description = "Create executable from source files without project")
public class CmdQuickBuild implements Runnable
{
	@Arguments(description = "Files to build")
	public List<String> fileNames;

	@Option(name = {"-Bm", "--print-build-metrics"}, description = "Print build metrics")
	public boolean printBuildMetrics;

	@Option(name = {"-Dt", "--diagnostic-traces"}, description = "Stack traces in diagnostics")
	public boolean diagnosticTraces;

	@Option(name = {"-Dedc", "--eval-dump-classfiles"}, description = "Dump class files generated for 'eval'")
	public boolean evalDumpClassFiles;

	private static Map<FileType, Set<Path>> fileNamesToSourceFiles(List<String> fileNames)
	{
		Map<FileType, Set<Path>> result = new EnumMap<>(FileType.class);

		for(var fileName : fileNames)
		{
			var type = FileType.of(fileName);

			if(type.isNone())
				throw new CompileException(String.format("unknown file type: %s", fileName));

			var files = result.computeIfAbsent(type.unwrap(), __ -> new HashSet<>());
			files.add(Paths.get(fileName));
		}

		return result;
	}

	@Override
	public void run()
	{
		if(fileNames.isEmpty())
			throw new CompileException("at least one file required");

		var name = FileUtils.stripExtension(fileNames.get(0));

		var context = new PlatformContext(TargetTriple.NATIVE);
		var diag = new DiagnosticsManager(diagnosticTraces);

		var target = new BuildTarget();
		target.type = BuildType.EXECUTABLE;
		target.entryPoint = name + ".main";
		target.name = name;
		target.sourceFiles = fileNamesToSourceFiles(fileNames);

		var options = new BuildOptions();
		options.printBuildMetrics = printBuildMetrics;
		options.evalDumpClassFiles = evalDumpClassFiles;

		try
		{
			var rootDir = Paths.get("").toAbsolutePath();
			var buildDir = Files.createTempDirectory("katana");

			try
			{
				var binaryPath = ProjectBuilder.buildTarget(diag, rootDir, buildDir, target, context, options);
				Files.copy(binaryPath, rootDir.resolve(binaryPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
			finally
			{
				FileUtils.deleteDirectory(buildDir);
			}
		}
		catch(CompileException ex)
		{
			if(diagnosticTraces)
				ex.printStackTrace();
			else
				System.err.println(ex.getMessage());

			System.exit(1);
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
	}
}
