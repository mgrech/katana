package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Expr;
import katana.ast.Type;

import java.util.Optional;

public class Variable extends Decl
{
	public Variable(boolean exported, boolean opaque, String name, Optional<Type> type, Expr init)
	{
		this.exported = exported;
		this.opaque = opaque;
		this.name = name;
		this.type = type;
		this.init = init;
	}

	public boolean exported;
	public boolean opaque;
	public String name;
	public Optional<Type> type;
	public Expr init;
}
