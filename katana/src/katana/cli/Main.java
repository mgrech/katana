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

package katana.cli;

import katana.cli.cmd.*;
import katana.utils.Rethrow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

public class Main
{
	public static final Map<String, Class<?>> COMMANDS = new TreeMap<>();

	private static void registerCommands()
	{
		Class<?>[] commands = new Class<?>[]{ CmdBuild.class, CmdCheader.class, CmdHelp.class, CmdInit.class, CmdVersion.class };

		for(Class<?> clazz : commands)
		{
			String name = clazz.getAnnotation(Command.class).name();
			COMMANDS.put(name, clazz);
		}
	}

	public static void main(String[] args) throws ReflectiveOperationException
	{
		registerCommands();

		if(args.length < 1)
		{
			System.err.println("missing command argument");
			System.exit(1);
		}

		Class<?> command = COMMANDS.get(args[0]);

		if(command == null)
		{
			System.err.print(String.format("unknown command '%s'. ", args[0]));
			CmdHelp.run(new String[0]);
			System.exit(1);
		}

		String[] commandArgs = new String[args.length - 1];
		System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

		try
		{
			Method run = command.getMethod("run", String[].class);
			run.invoke(null, (Object)commandArgs);
		}

		catch(InvocationTargetException ex)
		{
			Rethrow.of(ex.getTargetException());
		}

		catch(CommandException ex)
		{
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}
}
