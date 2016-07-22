package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Path;

public class Module extends Decl
{
	public Module(Path path)
	{
		this.path = path;
	}

	public Path path;
}
