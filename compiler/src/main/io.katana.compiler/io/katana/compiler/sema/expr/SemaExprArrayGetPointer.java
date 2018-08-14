package io.katana.compiler.sema.expr;

import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeArray;
import io.katana.compiler.sema.type.SemaTypeNonNullablePointer;

public class SemaExprArrayGetPointer extends SimpleRValueExpr
{
	public final SemaExpr expr;
	private final transient SemaType cachedType;

	public SemaExprArrayGetPointer(SemaExpr expr)
	{
		this.expr = expr;
		this.cachedType = new SemaTypeNonNullablePointer(((SemaTypeArray)expr.type()).type);
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}
}
