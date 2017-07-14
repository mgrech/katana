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

package katana.cli;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.help.Help;
import com.github.rvesse.airline.parser.errors.ParseException;
import katana.cli.cmd.CmdBuild;
import katana.cli.cmd.CmdInit;
import katana.cli.cmd.CmdVersion;

@com.github.rvesse.airline.annotations.Cli(
	name = "katana",
	description = "Katana compiler",
	defaultCommand = Help.class,
	commands = {CmdBuild.class, CmdInit.class, CmdVersion.class})
public class Main
{
	public static void main(String[] args)
	{
		Cli<Runnable> cli = new Cli<>(Main.class);

		try
		{
			cli.parse(args).run();
		}

		catch(ParseException ex)
		{
			System.err.println(ex.getMessage());
		}
	}
}
