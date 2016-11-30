package katana.ast.expr;

import katana.ast.type.AstType;

public class AstExprSignCast extends AstExpr
{
	public AstExprSignCast(AstType type, AstExpr expr)
	{
		this.type = type;
		this.expr = expr;
	}

	public AstType type;
	public AstExpr expr;
}
