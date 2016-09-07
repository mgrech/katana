package katana.sema.type;

import katana.backend.PlatformContext;
import katana.sema.Expr;
import katana.sema.Type;

public class Typeof extends Type
{
	public Typeof(Expr expr)
	{
		this.expr = expr;
	}

	@Override
	public int sizeof(PlatformContext context)
	{
		return expr.type().unwrap().sizeof(context);
	}

	@Override
	public int alignof(PlatformContext context)
	{
		return expr.type().unwrap().alignof(context);
	}

	@Override
	protected boolean same(Type other)
	{
		Type first = expr.type().unwrap();
		Type second = ((Typeof)other).expr.type().unwrap();
		return Type.same(first, second);
	}

	public Expr expr;
}
