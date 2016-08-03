package katana.ast;

import java.util.ArrayList;

public class File
{
	public File(java.nio.file.Path path, ArrayList<Decl> decls)
	{
		this.path = path;
		this.decls = decls;
	}

	public java.nio.file.Path path;
	public ArrayList<Decl> decls;
}
