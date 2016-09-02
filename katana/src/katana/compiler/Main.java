package katana.compiler;

import katana.compiler.commands.Build;
import katana.compiler.commands.Help;
import katana.compiler.commands.Init;
import katana.compiler.commands.Version;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

public class Main
{
	public static final Map<String, Class<?>> COMMANDS = new TreeMap<>();

	private static void registerCommands()
	{
		Class<?>[] commands = new Class<?>[]{ Build.class, Help.class, Init.class, Version.class };

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
			Help.run(new String[0]);
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
			try
			{
				throw (RuntimeException)ex.getTargetException();
			}

			catch(ClassCastException e)
			{
				throw new RuntimeException(ex.getTargetException());
			}
		}

		catch(CommandException ex)
		{
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}
}
