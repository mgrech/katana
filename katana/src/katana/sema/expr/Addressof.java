package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Builtin;

public class Addressof extends Expr
{
	public Addressof(Expr expr)
	{
		this.expr = expr;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(Builtin.PTR);
	}

	public Expr expr;
}
