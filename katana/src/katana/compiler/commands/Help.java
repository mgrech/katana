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

package katana.compiler.commands;

import katana.compiler.Command;
import katana.compiler.Main;

@Command(name = "help", desc = "prints this help message")
public class Help
{
	public static void run(String[] args)
	{
		System.err.print("available commands:\n");

		for(Class<?> clazz : Main.COMMANDS.values())
		{
			Command info = clazz.getAnnotation(Command.class);
			System.err.println(String.format("%s\t-- %s", info.name(), info.desc()));
		}
	}
}
