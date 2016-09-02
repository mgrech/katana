package katana.compiler.commands;

import katana.compiler.Command;
import katana.compiler.CommandException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Command(name = "init", desc = "initializes new project in working directory")
public class Init
{
	public static void run(String[] args) throws IOException
	{
		if(args.length != 0)
			throw new CommandException("invalid arguments, usage: katana init");

		Files.createDirectories(Paths.get("./source"));
		Files.createDirectories(Paths.get("./output"));
	}
}
