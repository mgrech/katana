package katana.sema;

import katana.ast.Decl;
import katana.ast.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Module
{
	public Module(String name, Path path, Module parent)
	{
		this.name = name;
		this.path = path;
		this.parent = parent;
	}

	public void defineSymbol(String name, Decl decl)
	{
		if(decls.containsKey(name))
			throw new RuntimeException("redefinition of symbol '" + name + "'");

		decls.put(name, decl);
	}

	public String name;
	public Path path;
	public Module parent;
	public Map<String, Module> children = new HashMap<>();
	public Map<String, Decl> decls = new HashMap<>();
}
