package katana.sema.expr;

import katana.Maybe;
import katana.sema.Type;
import katana.sema.type.Array;
import katana.sema.type.Builtin;

import java.nio.charset.StandardCharsets;

public class LitString extends SimpleLValueExpr
{
	public LitString(String value)
	{
		this.value = value;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(new Array(value.getBytes(StandardCharsets.UTF_8).length, Builtin.UINT8));
	}

	public String value;
}
