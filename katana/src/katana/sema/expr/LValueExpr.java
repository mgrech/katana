package katana.sema.expr;

import katana.sema.Expr;

public abstract class LValueExpr extends Expr
{
	public abstract boolean isUsedAsLValue();
	public abstract void useAsLValue(boolean use);
}
