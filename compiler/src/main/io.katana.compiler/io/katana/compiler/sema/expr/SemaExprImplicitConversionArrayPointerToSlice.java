package io.katana.compiler.sema.expr;

import io.katana.compiler.sema.type.SemaType;

public class SemaExprImplicitConversionArrayPointerToSlice extends SimpleRValueExpr
{
	public final SemaExpr expr;
	public final SemaType targetType;

	public SemaExprImplicitConversionArrayPointerToSlice(SemaExpr expr, SemaType targetType)
	{
		this.expr = expr;
		this.targetType = targetType;
	}

	@Override
	public SemaType type()
	{
		return targetType;
	}
}
