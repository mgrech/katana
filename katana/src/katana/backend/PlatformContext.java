package katana.backend;

import katana.Maybe;
import katana.sema.BuiltinFunc;
import katana.sema.decl.Data;
import katana.sema.type.Builtin;

public interface PlatformContext
{
	Maybe<BuiltinFunc> findBuiltin(String name);

	int sizeof(Builtin builtin);
	int sizeof(Data data);

	int alignof(Builtin builtin);
	int alignof(Data data);
}
