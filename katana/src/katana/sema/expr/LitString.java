package katana.sema.expr;

import katana.Maybe;
import katana.sema.Type;
import katana.sema.type.Array;
import katana.sema.type.Builtin;

public class LitString extends LValueExpr
{
	public LitString(String value)
	{
		this.value = value;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(new Array(value.length(), Builtin.UINT8));
	}

	public String value;
}
