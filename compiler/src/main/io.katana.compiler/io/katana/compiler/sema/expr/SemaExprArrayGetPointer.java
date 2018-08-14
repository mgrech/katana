package io.katana.compiler.sema.expr;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.type.SemaType;

public class SemaExprArrayGetPointer extends SimpleRValueExpr
{
	public final SemaExpr expr;
	private final transient SemaType cachedType;

	public SemaExprArrayGetPointer(SemaExpr expr)
	{
		this.expr = expr;
		this.cachedType = Types.addNonNullablePointer(Types.removeArray(expr.type()));
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}
}
