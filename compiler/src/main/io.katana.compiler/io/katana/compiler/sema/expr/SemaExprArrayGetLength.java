package io.katana.compiler.sema.expr;

import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;

public class SemaExprArrayGetLength extends SimpleRValueExpr
{
	public SemaExpr expr;

	public SemaExprArrayGetLength(SemaExpr expr)
	{
		this.expr = expr;
	}

	@Override
	public SemaType type()
	{
		return SemaTypeBuiltin.INT;
	}
}
