package katana.sema.expr;

import katana.sema.decl.SemaDeclRenamedImport;
import katana.sema.type.SemaType;

public class SemaExprNamedRenamedImport extends SemaExpr
{
	public SemaExprNamedRenamedImport(SemaDeclRenamedImport import_)
	{
		this.import_ = import_;
	}

	@Override
	public SemaType type()
	{
		throw new AssertionError("unreachable");
	}

	public SemaDeclRenamedImport import_;
}
