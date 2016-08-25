package katana.sema.stmt;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Stmt;

public class Return extends Stmt
{
	public Return(Maybe<Expr> ret)
	{
		this.ret = ret;
	}

	public Maybe<Expr> ret;
}
