package katana.sema.type;

import katana.backend.PlatformContext;
import katana.sema.Type;

public class Opaque extends Type
{
	public Opaque(int size, int alignment)
	{
		this.size = size;
		this.alignment = alignment;
	}

	@Override
	public int sizeof(PlatformContext context)
	{
		return size;
	}

	@Override
	public int alignof(PlatformContext context)
	{
		return alignment;
	}

	@Override
	protected boolean same(Type other)
	{
		Opaque o = (Opaque)other;
		return size == o.size && alignment == o.alignment;
	}

	@Override
	public String toString()
	{
		return String.format("opaque(%s, %s)", size, alignment);
	}

	public int size;
	public int alignment;
}
