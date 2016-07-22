package katana.ast.type;

import katana.ast.Type;

import java.util.ArrayList;
import java.util.Optional;

public class Function extends Type
{
	public Function(Optional<Type> ret, ArrayList<Type> params)
	{
		this.ret = ret;
		this.params = params;
	}

	public Optional<Type> ret;
	public ArrayList<Type> params;
}
