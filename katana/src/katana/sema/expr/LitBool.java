package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Builtin;

public class LitBool extends Expr
{
	public LitBool(boolean value)
	{
		this.value = value;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(Builtin.BOOL);
	}

	public boolean value;
}
