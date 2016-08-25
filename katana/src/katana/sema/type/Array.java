package katana.sema.type;

import katana.backend.PlatformContext;
import katana.sema.Type;

public class Array extends Type
{
	public Array(int length, Type type)
	{
		this.length = length;
		this.type = type;
	}

	@Override
	public int sizeof(PlatformContext context)
	{
		return length * type.sizeof(context);
	}

	@Override
	public int alignof(PlatformContext context)
	{
		return type.alignof(context);
	}

	@Override
	protected boolean same(Type other)
	{
		Array o = (Array)other;
		return length == o.length && Type.same(type, o.type);
	}

	@Override
	public String toString()
	{
		return String.format("[%s]%s", length, type);
	}

	public int length;
	public Type type;
}
