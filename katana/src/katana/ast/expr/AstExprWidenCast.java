package katana.ast.expr;

import katana.ast.type.AstType;

public class AstExprWidenCast extends AstExpr
{
	public AstExprWidenCast(AstType type, AstExpr expr)
	{
		this.type = type;
		this.expr = expr;
	}

	public AstType type;
	public AstExpr expr;
}
