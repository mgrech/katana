package io.katana.compiler.sema.expr;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.type.SemaType;

public class SemaExprImplicitConversionSliceToSliceOfConst extends SemaExpr
{
	public final SemaExpr expr;
	private final transient SemaType cachedType;

	public SemaExprImplicitConversionSliceToSliceOfConst(SemaExpr expr)
	{
		this.expr = expr;
		this.cachedType = Types.addSlice(Types.addConst(Types.removeSlice(expr.type())));
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
