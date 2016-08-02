package katana.ast.expr;

import katana.ast.Expr;
import katana.ast.Path;

import java.util.ArrayList;

public class BuiltinCall extends Expr
{
	public BuiltinCall(Path path, ArrayList<Expr> args)
	{
		this.path = path;
		this.args = args;
	}

	public Path path;
	public ArrayList<Expr> args;
}
