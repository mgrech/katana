package katana.ast;

import java.util.ArrayList;

public class Path
{
	public Path(ArrayList<String> components)
	{
		this.components = components;
	}

	public ArrayList<String> components;

	@Override
	public String toString()
	{
		return String.join(".", components);
	}
}
