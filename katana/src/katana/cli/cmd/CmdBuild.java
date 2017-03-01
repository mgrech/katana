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

package katana.cli.cmd;

import katana.analysis.ProgramValidator;
import katana.ast.AstProgram;
import katana.backend.PlatformContext;
import katana.backend.llvm.PlatformContextLlvm;
import katana.backend.llvm.ProgramCodeGenerator;
import katana.cli.Command;
import katana.cli.CommandException;
import katana.diag.CompileException;
import katana.diag.DiagnosticsManager;
import katana.parser.ProgramParser;
import katana.platform.TargetTriple;
import katana.project.FileType;
import katana.project.Project;
import katana.project.ProjectBuilder;
import katana.project.ProjectManager;
import katana.scanner.SourceManager;
import katana.sema.SemaProgram;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Command(name = "build", desc = "builds project at given path in the working directory")
public class CmdBuild
{
	@SuppressWarnings("unused")
	public static void run(String[] args) throws IOException
	{
		if(args.length != 1)
			throw new CommandException("invalid number of arguments, usage: build <source-dir>");

		PlatformContext context = new PlatformContextLlvm(TargetTriple.NATIVE);
		Path root = Paths.get(args[0]).toAbsolutePath().normalize();

		try
		{
			Project project = ProjectManager.load(root, context.target());
			Set<Path> katanaFiles = project.sourceFiles.get(FileType.KATANA);

			if(katanaFiles != null)
			{
				SourceManager sourceManager = SourceManager.loadFiles(project.root, project.sourceFiles.get(FileType.KATANA));

				DiagnosticsManager diag = new DiagnosticsManager(true);
				AstProgram ast = ProgramParser.parse(sourceManager, diag);

				if(!diag.successful())
				{
					diag.print();
					return;
				}

				SemaProgram program = ProgramValidator.validate(ast, context);
				ProgramCodeGenerator.generate(project, program, context);
			}

			ProjectBuilder.build(project, context.target());
		}

		catch(CompileException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
}
