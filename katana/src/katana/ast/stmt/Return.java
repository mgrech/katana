package katana.ast.stmt;

import katana.ast.Stmt;
import katana.ast.Expr;

public class Return extends Stmt
{
	public Return(Expr value)
	{
		this.value = value;
	}

	public Expr value;
}
