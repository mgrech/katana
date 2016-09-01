package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.decl.Data;

public class FieldAccessRValue extends Expr
{
	public FieldAccessRValue(Expr expr, Data.Field field)
	{
		this.expr = expr;
		this.field = field;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(field.type);
	}

	public Expr expr;
	public Data.Field field;
}
