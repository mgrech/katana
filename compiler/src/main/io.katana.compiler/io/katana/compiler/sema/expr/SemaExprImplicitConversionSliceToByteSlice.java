package io.katana.compiler.sema.expr;

import io.katana.compiler.sema.type.SemaType;

public class SemaExprImplicitConversionSliceToByteSlice extends SimpleRValueExpr
{
	public final SemaExpr nestedExpr;
	public final SemaType targetType;

	public SemaExprImplicitConversionSliceToByteSlice(SemaExpr nestedExpr, SemaType targetType)
	{
		this.nestedExpr = nestedExpr;
		this.targetType = targetType;
	}

	@Override
	public SemaType type()
	{
		return targetType;
	}
}
