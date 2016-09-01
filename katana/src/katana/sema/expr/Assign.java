package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;

public class Assign extends SimpleLValueExpr
{

	public Assign(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
	}

	@Override
	public Maybe<Type> type()
	{
		return left.type();
	}

	public Expr left;
	public Expr right;
}
