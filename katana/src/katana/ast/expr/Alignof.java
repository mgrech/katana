package katana.ast.expr;

import katana.ast.Expr;
import katana.ast.Type;

public class Alignof extends Expr
{
	public Alignof(Type type)
	{
		this.type = type;
	}

	public Type type;
}
