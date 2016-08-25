package katana.sema.type;

import katana.Maybe;
import katana.backend.PlatformContext;
import katana.sema.Type;

import java.util.Iterator;
import java.util.List;

public class Function extends Type
{
	public Function(Maybe<Type> ret, List<Type> params)
	{
		this.ret = ret;
		this.params = params;
	}

	@Override
	public int sizeof(PlatformContext context)
	{
		throw new UnsupportedOperationException("sizeof");
	}

	@Override
	public int alignof(PlatformContext context)
	{
		throw new UnsupportedOperationException("alignof");
	}

	@Override
	protected boolean same(Type other)
	{
		Function o = (Function)other;

		if(ret.isNone() != o.ret.isNone())
			return false;

		if(ret.isSome() && !Type.same(ret.unwrap(), o.ret.unwrap()))
			return false;

		if(params.size() != o.params.size())
			return false;

		for(Iterator<Type> it1 = params.iterator(), it2 = o.params.iterator(); it1.hasNext();)
		{
			Type t1 = it1.next();
			Type t2 = it2.next();

			if(!Type.same(t1, t2))
				return false;
		}

		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder paramString = new StringBuilder();

		if(!params.isEmpty())
		{
			paramString.append(params.get(0));

			for(int i = 1; i != params.size(); ++i)
			{
				paramString.append(", ");
				paramString.append(params.get(i));
			}
		}

		String retString = ret.isNone() ? "" : "=> " + ret.unwrap();

		return String.format("fn(%s)%s", paramString, retString);
	}

	public Maybe<Type> ret;
	public List<Type> params;
}
