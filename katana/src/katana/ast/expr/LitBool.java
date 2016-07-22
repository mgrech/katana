package katana.ast.expr;

import katana.ast.Expr;

public class LitBool extends Expr
{
	public LitBool(boolean value)
	{
		this.value = value;
	}

	public boolean value;
}
