package katana.sema.expr;

import katana.Maybe;
import katana.sema.Type;
import katana.sema.decl.ExternFunction;

import java.util.ArrayList;
import java.util.List;

public class NamedExternFunc extends SimpleLValueExpr
{
	public NamedExternFunc(ExternFunction func)
	{
		this.func = func;
	}

	@Override
	public Maybe<Type> type()
	{
		List<Type> params = new ArrayList<>();

		for(ExternFunction.Param param : func.params)
			params.add(param.type);

		return Maybe.some(new katana.sema.type.Function(func.ret, params));
	}

	public ExternFunction func;
}
