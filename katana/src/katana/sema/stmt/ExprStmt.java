package katana.sema.stmt;

import katana.sema.Expr;
import katana.sema.Stmt;

public class ExprStmt extends Stmt
{
	public ExprStmt(Expr expr)
	{
		this.expr = expr;
	}

	public Expr expr;
}
