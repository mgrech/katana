package katana.ast.type;

import katana.ast.Type;

public class UserDefined extends Type
{
	public UserDefined(String name)
	{
		this.name = name;
	}

	public String name;

	@Override
	public String toString()
	{
		return name;
	}
}
