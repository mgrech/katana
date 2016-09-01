package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.decl.Data;

public class FieldAccessLValue extends LValueExpr
{
	public FieldAccessLValue(LValueExpr expr, Data.Field field)
	{
		this.expr = expr;
		this.field = field;
	}

	@Override
	public void useAsLValue(boolean use)
	{
		expr.useAsLValue(use);
	}

	@Override
	public boolean isUsedAsLValue()
	{
		return expr.isUsedAsLValue();
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(field.type);
	}

	public LValueExpr expr;
	public Data.Field field;
}
