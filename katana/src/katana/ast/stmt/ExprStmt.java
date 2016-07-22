package katana.ast.stmt;

import katana.ast.Expr;
import katana.ast.Stmt;

public class ExprStmt extends Stmt
{
	public ExprStmt(Expr expr)
	{
		this.expr = expr;
	}

	public Expr expr;
}
