package katana.ast.expr;

import katana.ast.Expr;

public class Offsetof extends Expr
{
	public Offsetof(String type, String field)
	{
		this.type = type;
		this.field = field;
	}

	public String type;
	public String field;
}
