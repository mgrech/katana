// Copyright 2017 Markus Grech
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

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.backend.llvm.PlatformContextLlvm;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.DiagnosticsManager;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.project.Project;
import io.katana.compiler.project.ProjectBuilder;
import io.katana.compiler.project.ProjectManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Command(name = "build", description = "Build project")
public class CmdBuild implements Runnable
{
	@Option(name = {"-P", "--project-dir"}, description = "Project directory")
	@Required
	public String projectDir;

	@Option(name = {"-B", "--build-dir"}, description = "Build directory")
	public String buildDir;

	@Option(name = {"-Dt", "--diagnostic-traces"}, description = "Stack traces in diagnostics")
	public boolean diagnosticTraces;

	@Override
	public void run()
	{
		Path projectPath = Paths.get(projectDir).toAbsolutePath().normalize();
		Path buildPath = Paths.get(buildDir == null ? "" : buildDir).toAbsolutePath().normalize();

		try
		{
			PlatformContext context = new PlatformContextLlvm(TargetTriple.NATIVE);
			DiagnosticsManager diag = new DiagnosticsManager(diagnosticTraces);
			Project project = ProjectManager.load(projectPath, context.target());
			ProjectBuilder.build(diag, project, context, buildPath);
		}
		catch(CompileException | IOException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
}
