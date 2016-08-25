package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Function;

import java.util.List;

public class IndirectFunctionCall extends Expr
{
	public IndirectFunctionCall(Expr expr, List<Expr> args)
	{
		this.expr = expr;
		this.args = args;
	}

	@Override
	public Maybe<Type> type()
	{
		return ((Function)expr.type().unwrap()).ret;
	}

	public Expr expr;
	public List<Expr> args;
}
