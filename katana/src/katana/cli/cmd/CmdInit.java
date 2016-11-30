// Copyright 2016 Markus Grech
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

import katana.cli.Command;
import katana.cli.CommandException;
import katana.project.ProjectManager;

import java.io.IOException;
import java.nio.file.Paths;

@Command(name = "init", desc = "initializes new project in working directory")
public class CmdInit
{
	public static void run(String[] args) throws IOException
	{
		if(args.length != 0)
			throw new CommandException("invalid arguments, usage: katana init");

		ProjectManager.createDefaultProject(Paths.get(""));
	}
}
