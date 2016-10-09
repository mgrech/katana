package katana.sema.decl;

import katana.sema.SemaModule;

import java.util.HashMap;
import java.util.Map;

public class SemaDeclRenamedImport extends SemaDecl
{
	public SemaDeclRenamedImport(SemaModule module, String rename)
	{
		super(null, false, false);
		this.module = module;
		this.rename = rename;
	}

	@Override
	public String name()
	{
		return rename;
	}

	public SemaModule module;
	public String rename;
	public Map<String, SemaDecl> decls = new HashMap<>();
}
