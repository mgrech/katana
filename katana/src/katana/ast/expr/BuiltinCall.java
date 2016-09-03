package katana.ast.expr;

import katana.ast.Expr;

import java.util.ArrayList;

public class BuiltinCall extends Expr
{
	public BuiltinCall(String name, ArrayList<Expr> args)
	{
		this.name = name;
		this.args = args;
	}

	public String name;
	public ArrayList<Expr> args;
}
