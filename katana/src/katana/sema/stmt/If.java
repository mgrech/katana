package katana.sema.stmt;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Stmt;

public class If extends Stmt
{
	public If(Expr condition, Stmt then, Maybe<Stmt> otherwise)
	{
		this.condition = condition;
		this.then = then;
		this.otherwise = otherwise;
	}

	public Expr condition;
	public Stmt then;
	public Maybe<Stmt> otherwise;
}
