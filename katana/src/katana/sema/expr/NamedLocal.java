package katana.sema.expr;

import katana.Maybe;
import katana.sema.Type;
import katana.sema.decl.Function;

public class NamedLocal extends LValueExpr
{
	public NamedLocal(Function.Local local)
	{
		this.local = local;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(local.type);
	}

	public Function.Local local;
}
