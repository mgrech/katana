package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Path;

import java.util.Optional;

public class Import extends Decl
{
	public Import(Path path, Optional<String> rename)
	{
		super(false, false);
		this.path = path;
		this.rename = rename;
	}

	public Path path;
	public Optional<String> rename;

	@Override
	public String toString()
	{
		String rename = this.rename.orElse("<none>");
		return String.format("%s\tmodule: %s\n\trename: %s\n", super.toString(), path, rename);
	}
}
