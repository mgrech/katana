package katana.sema;

import katana.backend.PlatformContext;
import katana.visitor.IVisitable;

public abstract class Type implements IVisitable
{
	public abstract int sizeof(PlatformContext context);
	public abstract int alignof(PlatformContext context);

	protected abstract boolean same(Type other);

	public static boolean same(Type first, Type second)
	{
		if(first.getClass() != second.getClass())
			return false;

		return first.same(second);
	}
}
