// Copyright 2017-2018 Markus Grech
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
import io.katana.compiler.backend.llvm.PlatformContextLlvm;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.DiagnosticsManager;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.project.BuildTarget;
import io.katana.compiler.project.Project;
import io.katana.compiler.project.ProjectBuilder;
import io.katana.compiler.project.ProjectManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Command(name = "build", description = "Build project")
public class CmdBuild implements Runnable
{
	@Arguments(description = "Targets to build")
	public List<String> targets;

	@Option(name = {"-P", "--project-dir"}, description = "Project directory")
	public String projectDir;

	@Option(name = {"-B", "--build-dir"}, description = "Build directory")
	public String buildDir;

	@Option(name = {"-Bp", "--build-profiles"}, description = "Build profiles")
	public List<String> profiles;

	@Option(name = {"-Dt", "--diagnostic-traces"}, description = "Stack traces in diagnostics")
	public boolean diagnosticTraces;

	@Override
	public void run()
	{
		try
		{
			Path projectRoot = projectDir == null ? null : Paths.get(projectDir).toRealPath();

			if(projectRoot == null)
			{
				projectRoot = ProjectManager.locateProjectRoot();

				if(projectRoot == null)
					throw new CompileException("project root could not be found");
			}

			Path buildRoot = buildDir != null ? Paths.get(buildDir).toAbsolutePath().normalize()
				: projectDir == null ? projectRoot.resolve("build") : Paths.get("").toRealPath();

			PlatformContext context = new PlatformContextLlvm(TargetTriple.NATIVE);
			DiagnosticsManager diag = new DiagnosticsManager(diagnosticTraces);

			List<String> buildProfiles = profiles == null ? new ArrayList<>() : profiles;
			Project project = ProjectManager.load(projectRoot, buildRoot, new HashSet<>(buildProfiles), context.target());

			if(targets == null || targets.isEmpty())
			{
				for(Map.Entry<String, BuildTarget> entry : project.targets.entrySet())
					ProjectBuilder.build(diag, project.root, entry.getValue(), context);
			}
			else
			{
				for(String targetName : targets)
				{
					BuildTarget target = project.targets.get(targetName);

					if(target == null)
						throw new CompileException(String.format("unknown build target '%s'", targetName));

					ProjectBuilder.build(diag, project.root, target, context);
				}
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
			if(diagnosticTraces)
				ex.printStackTrace();
			else
				System.err.println(ex.getClass().getName() + ": " + ex.getMessage());

			System.exit(1);
		}
	}
}
