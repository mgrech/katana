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

package io.katana.compiler.cli.cmd;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import io.katana.compiler.project.ProjectManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Command(name = "init", description = "Initialize new project")
public class CmdInit implements Runnable
{
	@Option(name = {"-P", "--project-dir"}, description = "Project directory")
	public Path path;

	public void run()
	{
		try
		{
			if(path == null)
				path = Paths.get("");

			ProjectManager.createDefaultProject(path);
		}

		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
