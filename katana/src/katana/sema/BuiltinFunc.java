package katana.sema;

import katana.Maybe;
import katana.ast.Path;

import java.util.List;

public class BuiltinFunc
{
	public BuiltinFunc(Path name, Maybe<Type> ret, List<Type> params)
	{
		this.name = name;
		this.ret = ret;
		this.params = params;
	}

	public Path name;
	public Maybe<Type> ret;
	public List<Type> params;
}
