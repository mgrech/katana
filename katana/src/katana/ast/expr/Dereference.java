package katana.ast.expr;

import katana.ast.Expr;

public class Dereference extends Expr
{
	public Dereference(Expr expr)
	{
		this.expr = expr;
	}

	public Expr expr;
}
