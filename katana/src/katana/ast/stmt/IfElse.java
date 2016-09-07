package katana.ast.stmt;

import katana.ast.Expr;
import katana.ast.Stmt;

public class IfElse extends Stmt
{
	public IfElse(boolean negated, Expr condition, Stmt then, Stmt else_)
	{
		this.negated = negated;
		this.condition = condition;
		this.then = then;
		this.else_ = else_;
	}

	public boolean negated;
	public Expr condition;
	public Stmt then;
	public Stmt else_;
}
