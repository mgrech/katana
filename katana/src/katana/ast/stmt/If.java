package katana.ast.stmt;

import katana.ast.Stmt;
import katana.ast.Expr;

import java.util.Optional;

public class If extends Stmt
{
	public If(Expr condition, Stmt then, Optional<Stmt> otherwise)
	{
		this.condition = condition;
		this.then = then;
		this.otherwise = otherwise;
	}

	public Expr condition;
	public Stmt then;
	public Optional<Stmt> otherwise;
}
