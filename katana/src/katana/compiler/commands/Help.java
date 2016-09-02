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
