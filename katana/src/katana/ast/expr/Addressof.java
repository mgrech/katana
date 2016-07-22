package katana.ast.expr;

import katana.ast.Expr;

public class Addressof extends Expr
{
	public Addressof(Expr expr)
	{
		this.expr = expr;
	}

	public Expr expr;
}
