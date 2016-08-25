package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Builtin;

import java.math.BigDecimal;

public class LitFloat extends Expr
{
	public LitFloat(BigDecimal value)
	{
		this.value = value;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(Builtin.FLOAT64);
	}

	public BigDecimal value;
}
