package katana.sema.decl;

import katana.sema.Decl;
import katana.sema.Module;
import katana.sema.Type;

public class Global extends Decl
{
	public Global(Module module, String name)
	{
		super(module);
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
