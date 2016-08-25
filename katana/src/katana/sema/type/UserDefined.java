package katana.sema.type;

import katana.backend.PlatformContext;
import katana.sema.Type;
import katana.sema.decl.Data;

public class UserDefined extends Type
{
	public UserDefined(Data data)
	{
		this.data = data;
	}

	@Override
	public int sizeof(PlatformContext context)
	{
		return context.sizeof(data);
	}

	@Override
	public int alignof(PlatformContext context)
	{
		return context.alignof(data);
	}

	@Override
	protected boolean same(Type other)
	{
		return data == ((UserDefined)other).data;
	}

	@Override
	public String toString()
	{
		return data.qualifiedName().toString();
	}

	public Data data;
}
