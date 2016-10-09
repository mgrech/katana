package katana.sema.decl;

import katana.sema.SemaModule;
import katana.sema.SemaSymbol;
import katana.sema.type.SemaType;

public class SemaDeclTypeAlias extends SemaDecl implements SemaSymbol
{
	public SemaDeclTypeAlias(SemaModule module, boolean exported, String name)
	{
		super(module, exported, false);
		this.name = name;
	}

	@Override
	public String name()
	{
		return name;
	}

	public String name;
	public SemaType type;
}
