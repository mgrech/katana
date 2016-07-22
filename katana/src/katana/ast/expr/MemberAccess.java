package katana.ast.expr;

import katana.ast.Expr;

public class MemberAccess extends Expr
{
	public MemberAccess(Expr expr, String name)
	{
		this.expr = expr;
		this.name = name;
	}

	public Expr expr;
	public String name;
}
