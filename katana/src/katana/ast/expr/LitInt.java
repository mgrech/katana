package katana.ast.expr;

import katana.ast.Expr;

import java.math.BigInteger;

public class LitInt extends Expr
{
	public LitInt(BigInteger value)
	{
		this.value = value;
	}

	public BigInteger value;
}
