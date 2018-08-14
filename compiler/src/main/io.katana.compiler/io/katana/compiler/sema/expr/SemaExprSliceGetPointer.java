package io.katana.compiler.sema.expr;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeNonNullablePointer;

public class SemaExprSliceGetPointer extends SemaExpr
{
	public final SemaExpr expr;
	private final transient SemaType cachedType;

	public SemaExprSliceGetPointer(SemaExpr expr)
	{
		this.expr = expr;
		this.cachedType = new SemaTypeNonNullablePointer(Types.removeSlice(expr.type()));
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}

	@Override
	public ExprKind kind()
	{
		return expr.kind();
	}
}
