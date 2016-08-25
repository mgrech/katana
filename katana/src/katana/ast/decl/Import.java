package katana.ast.decl;

import katana.Maybe;
import katana.ast.Decl;
import katana.ast.Path;

public class Import extends Decl
{
	public Import(Path path, Maybe<String> rename)
	{
		super(false, false);
		this.path = path;
		this.rename = rename;
	}

	public Path path;
	public Maybe<String> rename;

	@Override
	public String toString()
	{
		String rename = this.rename.or("<none>");
		return String.format("%s\tmodule: %s\n\trename: %s\n", super.toString(), path, rename);
	}
}
