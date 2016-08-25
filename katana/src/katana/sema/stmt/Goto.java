package katana.sema.stmt;

import katana.sema.Stmt;

public class Goto extends Stmt
{
	public Goto(Label target)
	{
		this.target = target;
	}

	public Label target;
}
