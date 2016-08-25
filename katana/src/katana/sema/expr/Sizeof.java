package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Builtin;

public class Sizeof extends Expr
{
	public Sizeof(Type type)
	{
		this.type = type;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(Builtin.INT);
	}

	public Type type;
}
