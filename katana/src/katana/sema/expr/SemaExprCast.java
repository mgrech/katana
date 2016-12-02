package katana.sema.expr;

import katana.sema.type.SemaType;

public class SemaExprCast extends SemaExpr
{
	public enum Kind
	{
		SIGN_CAST,
		WIDEN_CAST,
		NARROW_CAST,
		POINTEGER_CAST,
	}

	public SemaExprCast(SemaType type, SemaExpr expr, Kind kind)
	{
		this.type = type;
		this.expr = expr;
		this.kind = kind;
	}

	@Override
	public SemaType type()
	{
		return type;
	}

	public SemaType type;
	public SemaExpr expr;
	public Kind kind;
}
