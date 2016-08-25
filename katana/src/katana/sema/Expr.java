package katana.sema;

import katana.Maybe;
import katana.visitor.IVisitable;

public abstract class Expr implements IVisitable
{
	public abstract Maybe<Type> type();
}
