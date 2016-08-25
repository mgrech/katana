package katana.ast;

import katana.visitor.IVisitable;

public abstract class Stmt implements IVisitable
{
	@Override
	public String toString()
	{
		return getClass().getName();
	}
}
