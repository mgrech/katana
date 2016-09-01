package katana.sema.expr;

import katana.Maybe;
import katana.sema.Type;
import katana.sema.decl.Function;

public class NamedParam extends SimpleLValueExpr
{
	public NamedParam(Function.Param param)
	{
		this.param = param;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(param.type);
	}

	public Function.Param param;
}
