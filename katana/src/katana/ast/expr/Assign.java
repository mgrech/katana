package katana.ast.expr;

import katana.ast.Expr;

public class Assign extends Expr
{
	public Assign(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
	}

	public Expr left;
	public Expr right;
}
