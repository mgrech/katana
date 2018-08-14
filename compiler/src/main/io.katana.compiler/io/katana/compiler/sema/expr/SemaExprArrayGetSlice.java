package io.katana.compiler.sema.expr;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.type.SemaType;

public class SemaExprArrayGetSlice extends SimpleRValueExpr
{
	public final SemaExpr expr;
	private final transient SemaType cachedType;

	public SemaExprArrayGetSlice(SemaExpr expr)
	{
		this.expr = expr;
		this.cachedType = Types.addSlice(Types.removeArray(expr.type()));
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}
}
