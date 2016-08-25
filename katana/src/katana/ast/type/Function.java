package katana.ast.type;

import katana.Maybe;
import katana.ast.Type;

import java.util.ArrayList;

public class Function extends Type
{
	public Function(Maybe<Type> ret, ArrayList<Type> params)
	{
		this.ret = ret;
		this.params = params;
	}

	public Maybe<Type> ret;
	public ArrayList<Type> params;
}
