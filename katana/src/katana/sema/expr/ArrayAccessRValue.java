package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Array;

public class ArrayAccessRValue extends Expr
{
	public ArrayAccessRValue(Expr expr, Expr index)
	{
		this.expr = expr;
		this.index = index;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(((Array)(expr.type().unwrap())).type);
	}

	public Expr expr;
	public Expr index;
}
