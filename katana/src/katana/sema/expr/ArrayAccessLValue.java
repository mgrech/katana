package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Array;

public class ArrayAccessLValue extends LValueExpr
{
	public ArrayAccessLValue(LValueExpr value, Expr index)
	{
		this.value = value;
		this.index = index;
	}

	@Override
	public void useAsLValue(boolean use)
	{
		value.useAsLValue(use);
	}

	@Override
	public boolean isUsedAsLValue()
	{
		return value.isUsedAsLValue();
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(((Array)value.type().unwrap()).type);
	}

	public LValueExpr value;
	public Expr index;
}
