package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Builtin;

public class Alignof extends Expr
{
	public Alignof(Type type)
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
