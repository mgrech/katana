package katana.ast.expr;

import katana.ast.Expr;

public class LitString extends Expr
{
	public LitString(String value)
	{
		this.value = value;
	}

	public String value;
}
