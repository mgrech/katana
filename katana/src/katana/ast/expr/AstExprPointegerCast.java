package katana.ast.expr;

import katana.ast.type.AstType;

public class AstExprPointegerCast extends AstExpr
{
	public AstExprPointegerCast(AstType type, AstExpr expr)
	{
		this.type = type;
		this.expr = expr;
	}

	public AstType type;
	public AstExpr expr;
}
