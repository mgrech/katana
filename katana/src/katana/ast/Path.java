package katana.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Path
{
	public Path()
	{
		this.components = new ArrayList<>();
	}

	public Path(String... components)
	{
		this.components = Arrays.asList(components);
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

	@Override
	public boolean equals(Object obj)
	{
		Path other = (Path)obj;
		return components.equals(other.components);
	}

	@Override
	public int hashCode()
	{
		return components.hashCode();
	}
}
