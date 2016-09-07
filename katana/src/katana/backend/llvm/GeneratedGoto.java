package katana.backend.llvm;

import katana.sema.Stmt;

public class GeneratedGoto extends Stmt
{
	public GeneratedGoto(GeneratedLabel label)
	{
		this.label = label;
	}

	public GeneratedLabel label;
}
