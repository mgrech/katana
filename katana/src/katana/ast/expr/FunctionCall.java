package katana.ast.expr;

import katana.ast.Expr;

import java.util.ArrayList;
import java.util.Optional;

public class FunctionCall extends Expr
{
	public FunctionCall(Expr expr, ArrayList<Expr> args, Optional<Boolean> inline)
	{
		this.expr = expr;
		this.args = args;
		this.inline = inline;
	}

	public Expr expr;
	public ArrayList<Expr> args;
	public Optional<Boolean> inline;
}
