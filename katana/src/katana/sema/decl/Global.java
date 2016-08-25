package katana.sema.decl;

import katana.sema.Decl;
import katana.sema.Module;
import katana.sema.Type;

public class Global extends Decl
{
	public Global(Module module, String name, Type type)
	{
		super(module);
		this.name = name;
		this.type = type;
	}

	@Override
	public String name()
	{
		return name;
	}

	public String name;
	public Type type;
}
