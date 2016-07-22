package katana.ast.expr;

import katana.ast.Expr;

public class NamedValue extends Expr
{
	public NamedValue(String name)
	{
		this.name = name;
	}

	public String name;
}
