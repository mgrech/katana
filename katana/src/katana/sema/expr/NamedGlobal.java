package katana.sema.expr;

import katana.Maybe;
import katana.sema.Type;
import katana.sema.decl.Global;

public class NamedGlobal extends SimpleLValueExpr
{
	public NamedGlobal(Global global)
	{
		this.global = global;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(global.type);
	}

	public Global global;
}
