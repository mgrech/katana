package katana.sema.expr;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.decl.Function;

import java.util.List;

public class DirectFunctionCall extends Expr
{
	public DirectFunctionCall(Function function, List<Expr> args, Maybe<Boolean> inline)
	{
		this.function = function;
		this.args = args;
		this.inline = inline;
	}

	@Override
	public Maybe<Type> type()
	{
		return function.ret;
	}

	public Function function;
	public List<Expr> args;
	public Maybe<Boolean> inline;
}
