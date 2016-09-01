package katana.sema.expr;

public abstract class SimpleLValueExpr extends LValueExpr
{
	@Override
	public boolean isUsedAsLValue()
	{
		return usedAsLValue;
	}

	@Override
	public void useAsLValue(boolean use)
	{
		usedAsLValue = use;
	}

	private boolean usedAsLValue = false;
}
