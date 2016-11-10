package katana.sema.decl;

import katana.op.Operator;
import katana.sema.SemaModule;

public class SemaDeclDefinedOperator extends SemaDeclDefinedFunction
{
	public SemaDeclDefinedOperator(SemaModule module, boolean exported, boolean opaque, SemaDeclOperator decl)
	{
		super(module, exported, opaque, Operator.implName(decl.operator.symbol, decl.operator.kind));
		this.decl = decl;
	}

	public SemaDeclOperator decl;
}
