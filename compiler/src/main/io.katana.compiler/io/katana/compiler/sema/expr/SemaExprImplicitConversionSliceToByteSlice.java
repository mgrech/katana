package io.katana.compiler.sema.expr;

import io.katana.compiler.sema.type.SemaType;

public class SemaExprImplicitConversionSliceToByteSlice extends SimpleRValueExpr
{
	public final SemaExpr expr;
	public final SemaType type;

	public SemaExprImplicitConversionSliceToByteSlice(SemaExpr expr, SemaType type)
	{
		this.expr = expr;
		this.type = type;
	}

	@Override
	public SemaType type()
	{
		return type;
	}
}
