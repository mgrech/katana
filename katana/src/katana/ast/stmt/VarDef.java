package katana.ast.stmt;


import katana.ast.Expr;
import katana.ast.Stmt;

public class VarDef extends Stmt
{
	public VarDef(String name, Expr init)
	{
		this.name = name;
		this.init = init;
	}

	public String name;
	public Expr init;
}
