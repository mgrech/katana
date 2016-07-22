package katana.ast.type;

import katana.ast.Type;

public class Pointer extends Type
{
	public Pointer(Type type)
	{
		this.type = type;
	}

	public Type type;
}
