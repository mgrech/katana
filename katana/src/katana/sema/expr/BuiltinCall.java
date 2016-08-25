package katana.sema.expr;

import katana.Maybe;
import katana.sema.BuiltinFunc;
import katana.sema.Expr;
import katana.sema.Type;

import java.util.List;

public class BuiltinCall extends Expr
{
	public BuiltinCall(BuiltinFunc func, List<Expr> args)
	{
		this.func = func;
		this.args = args;
	}

	@Override
	public Maybe<Type> type()
	{
		return func.ret;
	}

	public BuiltinFunc func;
	public List<Expr> args;
}
