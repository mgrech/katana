package katana.ast.type;

import katana.ast.Type;

public class Opaque extends Type
{
	public Opaque(int size, int alignment)
	{
		this.size = size;
		this.alignment = alignment;
	}

	public int size;
	public int alignment;
}
