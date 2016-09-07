package katana.backend.llvm;

import katana.Maybe;
import katana.sema.Expr;
import katana.sema.Type;

public class SSAExpr extends Expr
{
	public SSAExpr(String name)
	{
		this.name = name;
	}

	@Override
	public Maybe<Type> type()
	{
		return null;
	}

	public String name;
}
