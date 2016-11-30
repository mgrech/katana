package katana.ast.expr;

import katana.ast.type.AstType;

public class AstExprNarrowCast extends AstExpr
{
	public AstExprNarrowCast(AstType type, AstExpr expr)
	{
		this.type = type;
		this.expr = expr;
	}

	public AstType type;
	public AstExpr expr;
}
