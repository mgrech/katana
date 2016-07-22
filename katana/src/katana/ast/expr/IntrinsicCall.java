package katana.ast.expr;

import katana.ast.Expr;

import java.util.ArrayList;

public class IntrinsicCall extends Expr
{
	public IntrinsicCall(String name, ArrayList<Expr> args)
	{
		this.name = name;
		this.args = args;
	}

	public String name;
	public ArrayList<Expr> args;
}
