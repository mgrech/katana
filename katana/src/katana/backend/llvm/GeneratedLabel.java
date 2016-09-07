package katana.backend.llvm;

import katana.sema.Stmt;

public class GeneratedLabel extends Stmt
{
	public GeneratedLabel(String name)
	{
		this.name = name;
	}

	public String name;
}
