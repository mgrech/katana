package katana.ast.expr;

import katana.Maybe;
import katana.ast.Expr;

import java.util.ArrayList;

public class FunctionCall extends Expr
{
	public FunctionCall(Expr expr, ArrayList<Expr> args, Maybe<Boolean> inline)
	{
		this.expr = expr;
		this.args = args;
		this.inline = inline;
	}

	public Expr expr;
	public ArrayList<Expr> args;
	public Maybe<Boolean> inline;
}
