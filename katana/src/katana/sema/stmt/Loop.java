package katana.sema.stmt;

import katana.sema.Stmt;

public class Loop extends Stmt
{
	public Loop(Stmt body)
	{
		this.body = body;
	}

	public Stmt body;
}
