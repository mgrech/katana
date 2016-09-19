package katana.sema.expr;

import katana.sema.decl.RenamedImport;
import katana.sema.type.Type;
import katana.utils.Maybe;

public class NamedRenamedImport extends Expr
{
	public NamedRenamedImport(RenamedImport import_)
	{
		this.import_ = import_;
	}

	@Override
	public Maybe<Type> type()
	{
		return null;
	}

	public RenamedImport import_;
}
