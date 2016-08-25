package katana.backend;

import katana.Maybe;
import katana.ast.Path;
import katana.sema.BuiltinFunc;
import katana.sema.Type;
import katana.sema.decl.Data;
import katana.sema.type.Builtin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class PlatformContext
{
	private static final Map<Path, BuiltinFunc> BUILTINS = new HashMap<>();

	private static void registerBuiltin(String name, Type ret, Type... params)
	{
		Path path = new Path(name.split("\\."));
		BUILTINS.put(path, new BuiltinFunc(path, Maybe.some(ret), Arrays.asList(params)));
	}

	static
	{
		registerBuiltin("std.less.i", Builtin.BOOL, Builtin.INT, Builtin.INT);
		registerBuiltin("std.add.i", Builtin.INT, Builtin.INT, Builtin.INT);
	}

	protected Maybe<BuiltinFunc> findBuiltinFallback(Path path)
	{
		return Maybe.none();
	}

	public Maybe<BuiltinFunc> findBuiltin(Path path)
	{
		BuiltinFunc func = BUILTINS.get(path);

		if(func == null)
			return findBuiltinFallback(path);

		return Maybe.some(func);
	}

	public abstract int sizeof(Builtin builtin);
	public abstract int sizeof(Data data);

	public abstract int alignof(Builtin builtin);
	public abstract int alignof(Data data);
}
