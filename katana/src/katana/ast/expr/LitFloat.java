package katana.ast.expr;

import katana.ast.Expr;

import java.math.BigDecimal;

public class LitFloat extends Expr
{
	public LitFloat(BigDecimal value)
	{
		this.value = value;
	}

	public BigDecimal value;
}
