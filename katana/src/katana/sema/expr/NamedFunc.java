package katana.sema.expr;

import katana.Maybe;
import katana.sema.Type;
import katana.sema.decl.Function;

import java.util.ArrayList;
import java.util.List;

public class NamedFunc extends LValueExpr
{
	public NamedFunc(Function func)
	{
		this.func = func;
	}

	@Override
	public Maybe<Type> type()
	{
		List<Type> params = new ArrayList<>();

		for(Function.Param param : func.params)
			params.add(param.type);

		return Maybe.some(new katana.sema.type.Function(func.ret, params));
	}

	public Function func;
}
