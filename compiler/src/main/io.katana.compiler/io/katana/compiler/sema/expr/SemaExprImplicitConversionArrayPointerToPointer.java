package io.katana.compiler.sema.expr;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.type.SemaType;

public class SemaExprImplicitConversionArrayPointerToPointer extends SimpleRValueExpr
{
	public final SemaExpr expr;
	private final transient SemaType cachedType;

	public SemaExprImplicitConversionArrayPointerToPointer(SemaExpr expr)
	{
		this.expr = expr;
		var elementType = Types.removeArray(Types.removePointer(expr.type()));
		this.cachedType = Types.copyPointerKind(expr.type(), elementType);
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}
}
