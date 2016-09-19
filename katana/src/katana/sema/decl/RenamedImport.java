package katana.sema.decl;

import katana.sema.Module;

import java.util.HashMap;
import java.util.Map;

public class RenamedImport extends Decl
{
	public RenamedImport(Module module, String rename)
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

	public Module module;
	public String rename;
	public Map<String, Decl> decls = new HashMap<>();
}
