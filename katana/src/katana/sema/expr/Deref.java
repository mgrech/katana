package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;

public class Deref extends SimpleLValueExpr
{
	public Deref(Type type, Expr expr)
	{
		this.type = type;
		this.expr = expr;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(type);
	}

	public Type type;
	public Expr expr;
}
