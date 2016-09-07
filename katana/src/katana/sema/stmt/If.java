package katana.sema.stmt;

import katana.sema.Expr;
import katana.sema.Stmt;

public class If extends Stmt
{
	public If(boolean negated, Expr condition, Stmt then)
	{
		this.negated = negated;
		this.condition = condition;
		this.then = then;
	}

	public boolean negated;
	public Expr condition;
	public Stmt then;
}
