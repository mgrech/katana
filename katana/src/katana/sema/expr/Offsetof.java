package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.decl.Data;
import katana.sema.type.Builtin;

public class Offsetof extends Expr
{
	public Offsetof(Data.Field field)
	{
		this.field = field;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(Builtin.INT);
	}

	public Data.Field field;
}
