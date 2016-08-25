package katana.sema.stmt;

import katana.sema.Stmt;

public class Label extends Stmt
{
	public Label(String name)
	{
		this.name = name;
	}

	public String name;
}
