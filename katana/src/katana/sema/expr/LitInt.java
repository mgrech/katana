package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Builtin;

import java.math.BigInteger;

public class LitInt extends Expr
{
	public LitInt(BigInteger value)
	{
		this.value = value;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(Builtin.INT);
	}

	public BigInteger value;
}
