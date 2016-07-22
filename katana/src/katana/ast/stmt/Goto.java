package katana.ast.stmt;

import katana.ast.Stmt;

public class Goto extends Stmt
{
	public Goto(String label)
	{
		this.label = label;
	}

	public String label;
}
