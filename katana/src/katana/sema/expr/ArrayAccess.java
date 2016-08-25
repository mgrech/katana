package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Array;

public class ArrayAccess extends LValueExpr
{
	public ArrayAccess(Expr value, Expr index)
	{
		this.value = value;
		this.index = index;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(((Array)value.type().unwrap()).type);
	}

	public Expr value;
	public Expr index;
}
