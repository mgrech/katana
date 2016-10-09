package katana.sema.expr;

import katana.sema.decl.SemaDeclRenamedImport;
import katana.sema.type.SemaType;
import katana.utils.Maybe;

public class SemaExprNamedRenamedImport extends SemaExpr
{
	public SemaExprNamedRenamedImport(SemaDeclRenamedImport import_)
	{
		this.import_ = import_;
	}

	@Override
	public Maybe<SemaType> type()
	{
		return null;
	}

	public SemaDeclRenamedImport import_;
}
