package katana.sema.decl;

import katana.Maybe;
import katana.sema.Decl;
import katana.sema.Module;
import katana.sema.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternFunction extends Decl
{
	public class Param
	{
		public Param(Type type, String name, int index)
		{
			this.type = type;
			this.name = name;
			this.index = index;
		}

		public Type type;
		public String name;
		public int index;
	}

	public ExternFunction(Module module, String externName, String name, Maybe<Type> ret)
	{
		super(module);
		this.externName = externName;
		this.name = name;
		this.ret = ret;
	}

	public boolean defineParam(Type type, String name)
	{
		if(paramsByName.containsKey(name))
			return false;

		Param param = new Param(type, name, params.size());
		params.add(param);
		paramsByName.put(name, param);
		return true;
	}

	@Override
	public String name()
	{
		return name;
	}

	public String externName;
	private String name;
	public List<Param> params = new ArrayList<>();
	public Map<String, Param> paramsByName = new HashMap<>();
	public Maybe<Type> ret;
}
