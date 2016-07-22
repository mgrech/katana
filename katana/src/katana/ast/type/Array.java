package katana.ast.type;

import katana.ast.Type;

public class Array extends Type
{
	public Array(int size, Type type)
	{
		this.size = size;
		this.type = type;
	}

	public int size;
	public Type type;
}
