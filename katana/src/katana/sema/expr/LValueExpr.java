package katana.sema.expr;

import katana.sema.Expr;

public abstract class LValueExpr extends Expr
{
	public boolean usedAsLValue = false;
}
