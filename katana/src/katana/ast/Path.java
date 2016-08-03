package katana.ast;

import java.util.ArrayList;
import java.util.List;

public class Path
{
	public Path()
	{
		this.components = new ArrayList<>();
	}

	public Path(List<String> components)
	{
		this.components = components;
	}

	public List<String> components;

	@Override
	public String toString()
	{
		return String.join(".", components);
	}
}
