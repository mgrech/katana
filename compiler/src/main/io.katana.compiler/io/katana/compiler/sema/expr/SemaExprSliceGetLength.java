package io.katana.compiler.sema.expr;

import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;

public class SemaExprSliceGetLength extends SemaExpr
{
	public SemaExpr expr;

	public SemaExprSliceGetLength(SemaExpr expr)
	{
		this.expr = expr;
	}

	@Override
	public SemaType type()
	{
		return SemaTypeBuiltin.INT;
	}

	@Override
	public ExprKind kind()
	{
		return expr.kind();
	}
}
