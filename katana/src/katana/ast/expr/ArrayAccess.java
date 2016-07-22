package katana.ast.expr;

import katana.ast.Expr;

public class ArrayAccess extends Expr
{
	public ArrayAccess(Expr value, Expr index)
	{
		this.value = value;
		this.index = index;
	}

	public Expr value;
	public Expr index;
}
