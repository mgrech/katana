package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Builtin;

public class LitNull extends Expr
{
	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(Builtin.PTR);
	}
}
