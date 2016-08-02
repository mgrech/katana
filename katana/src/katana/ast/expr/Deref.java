package katana.ast.expr;

import katana.ast.Expr;
import katana.ast.Type;

public class Deref extends Expr
{
	public Deref(Type type, Expr expr)
	{
		this.type = type;
		this.expr = expr;
	}

	public Type type;
	public Expr expr;
}
