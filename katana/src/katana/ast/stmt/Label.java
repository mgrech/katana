package katana.ast.stmt;

import katana.ast.Stmt;

public class Label extends Stmt
{
	public Label(String name)
	{
		this.name = name;
	}

	public String name;
}
