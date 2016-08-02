package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Path;

public class Module extends Decl
{
	public Module(Path path)
	{
		super(false, false);
		this.path = path;
	}

	public Path path;

	@Override
	public String toString()
	{
		return String.format("%s\tname: %s\n", super.toString(), path);
	}
}
