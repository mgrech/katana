package katana.sema.stmt;

import katana.sema.Expr;
import katana.sema.Stmt;

public class If extends Stmt
{
	public If(Expr condition, Stmt then)
	{
		this.condition = condition;
		this.then = then;
	}

	public Expr condition;
	public Stmt then;
}
