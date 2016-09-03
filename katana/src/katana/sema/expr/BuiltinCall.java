package katana.sema.expr;

import katana.Maybe;
import katana.sema.BuiltinFunc;
import katana.sema.Expr;
import katana.sema.Type;

import java.util.List;

public class BuiltinCall extends Expr
{
	public BuiltinCall(BuiltinFunc func, List<Expr> args, Maybe<Type> ret)
	{
		this.func = func;
		this.args = args;
		this.ret = ret;
	}

	@Override
	public Maybe<Type> type()
	{
		return ret;
	}

	public BuiltinFunc func;
	public List<Expr> args;
	public Maybe<Type> ret;
}
