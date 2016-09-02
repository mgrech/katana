package katana.compiler.commands;

import katana.compiler.Command;

@Command(name = "version", desc = "prints out version information")
public class Version
{
	public static final String VERSION = "0.1";

	public static void run(String[] args)
	{
		System.out.println(String.format("katana version %s\nby Markus Grech\n", VERSION));
	}
}
