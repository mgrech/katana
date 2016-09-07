package katana.ast.stmt;

import katana.ast.Stmt;

public class Loop extends Stmt
{
	public Loop(Stmt body)
	{
		this.body = body;
	}

	public Stmt body;
}
