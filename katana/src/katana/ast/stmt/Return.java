package katana.ast.stmt;

import katana.Maybe;
import katana.ast.Expr;
import katana.ast.Stmt;

public class Return extends Stmt
{
	public Return(Maybe<Expr> value)
	{
		this.value = value;
	}

	public Maybe<Expr> value;
}
