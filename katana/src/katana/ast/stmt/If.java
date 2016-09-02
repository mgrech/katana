package katana.ast.stmt;

import katana.ast.Expr;
import katana.ast.Stmt;

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
