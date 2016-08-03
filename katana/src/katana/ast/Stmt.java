package katana.ast;

import katana.ast.visitor.IVisitable;

public abstract class Stmt implements IVisitable
{
	@Override
	public String toString()
	{
		return getClass().getName();
	}
}
