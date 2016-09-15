package katana.ast.decl;

import katana.ast.type.Type;

public class TypeAlias extends Decl
{
	public TypeAlias(boolean exported, String name, Type type)
	{
		super(exported, false);
		this.name = name;
		this.type = type;
	}

	public String name;
	public Type type;
}
