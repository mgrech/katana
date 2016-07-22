package katana.ast.expr;

import katana.ast.Expr;
import katana.ast.Type;

public class Sizeof extends Expr
{
	public Sizeof(Type type)
	{
		this.type = type;
	}

	public Type type;
}
