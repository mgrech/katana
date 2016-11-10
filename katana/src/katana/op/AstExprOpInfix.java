package katana.op;

import katana.ast.expr.AstExpr;
import katana.sema.decl.SemaDeclOperator;

public class AstExprOpInfix extends AstExpr
{
	public AstExpr left;
	public AstExpr right;
	public SemaDeclOperator decl;

	public AstExprOpInfix(AstExpr left, AstExpr right, SemaDeclOperator decl)
	{
		this.left = left;
		this.right = right;
		this.decl = decl;
	}
}
