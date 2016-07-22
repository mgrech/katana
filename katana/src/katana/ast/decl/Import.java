package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Path;

import java.util.Optional;

public class Import extends Decl
{
	public Import(Path path, Optional<String> rename)
	{
		this.path = path;
		this.rename = rename;
	}

	public Path path;
	public Optional<String> rename;
}
