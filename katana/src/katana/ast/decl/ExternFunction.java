package katana.ast.decl;

import katana.Maybe;
import katana.ast.Decl;
import katana.ast.Type;

import java.util.List;

public class ExternFunction extends Decl
{
	public ExternFunction(boolean exported, boolean opaque, String externName, String name, List<Function.Param> params, Maybe<Type> ret)
	{
		super(exported, opaque);
		this.externName = externName;
		this.name = name;
		this.params = params;
		this.ret = ret;
	}

	public String externName;
	public String name;
	public List<Function.Param> params;
	public Maybe<Type> ret;
}
