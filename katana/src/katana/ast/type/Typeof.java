package katana.ast.type;

import katana.ast.Expr;
import katana.ast.Type;

public class Typeof extends Type
{
	public Typeof(Expr expr)
	{
		this.expr = expr;
	}

	public Expr expr;
}
