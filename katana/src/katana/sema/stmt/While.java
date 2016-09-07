package katana.sema.stmt;

import katana.sema.Expr;
import katana.sema.Stmt;

public class While extends Stmt
{
	public While(boolean negated, Expr condition, Stmt body)
	{
		this.negated = negated;
		this.condition = condition;
		this.body = body;
	}

	public boolean negated;
	public Expr condition;
	public Stmt body;
}
