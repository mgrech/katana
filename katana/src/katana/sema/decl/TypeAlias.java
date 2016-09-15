package katana.sema.decl;

import katana.sema.Module;
import katana.sema.Symbol;
import katana.sema.type.Type;

public class TypeAlias extends Decl implements Symbol
{
	public TypeAlias(Module module, boolean exported, String name)
	{
		super(module, exported, false);
		this.name = name;
	}

	@Override
	public String name()
	{
		return name;
	}

	public String name;
	public Type type;
}
