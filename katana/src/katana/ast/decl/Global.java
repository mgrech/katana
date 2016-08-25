package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Type;

public class Global extends Decl
{
	public Global(boolean exported, boolean opaque, Type type, String name)
	{
		super(exported, opaque);
		this.type = type;
		this.name = name;
	}

	public Type type;
	public String name;

	@Override
	public String toString()
	{
		return String.format("%s\ttype: %s\n\tname: %s\n", super.toString(), type, name);
	}
}
