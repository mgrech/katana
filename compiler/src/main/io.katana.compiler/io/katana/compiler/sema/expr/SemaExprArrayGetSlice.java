package io.katana.compiler.sema.expr;

import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeArray;
import io.katana.compiler.sema.type.SemaTypeSlice;

public class SemaExprArrayGetSlice extends SimpleRValueExpr
{
	public final SemaExpr expr;
	private final transient SemaType cachedType;

	public SemaExprArrayGetSlice(SemaExpr expr)
	{
		this.expr = expr;
		this.cachedType = new SemaTypeSlice(((SemaTypeArray)expr.type()).type);
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}
}
