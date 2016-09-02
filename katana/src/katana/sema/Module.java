package katana.sema;

import katana.Maybe;
import katana.ast.Path;
import katana.sema.decl.Data;
import katana.sema.decl.ExternFunction;
import katana.sema.decl.Function;
import katana.sema.decl.Global;

import java.util.Map;
import java.util.TreeMap;

public class Module
{
	public Module(String name, Path path, Module parent)
	{
		this.name = name;
		this.path = path;
		this.parent = parent;
	}

	public boolean defineData(Data data)
	{
		if(!defineSymbol(data))
			return false;

		datas.put(data.name(), data);
		return true;
	}

	public boolean defineGlobal(Global global)
	{
		if(!defineSymbol(global))
			return false;

		globals.put(global.name, global);
		return true;
	}

	public boolean defineFunction(Function function)
	{
		if(!defineSymbol(function))
			return false;

		functions.put(function.name(), function);
		return true;
	}

	public boolean defineExternFunction(ExternFunction function)
	{
		if(!defineSymbol(function))
			return false;

		externFunctions.put(function.name(), function);
		return true;
	}

	private boolean defineSymbol(Decl decl)
	{
		if(decls.containsKey(decl.name()))
			return false;

		decls.put(decl.name(), decl);
		return true;
	}

	public Maybe<Decl> findSymbol(String name)
	{
		Module current = this;
		Decl decl;

		do
		{
			decl = decls.get(name);
			current = current.parent;
		}

		while(decl == null && current != null);

		return Maybe.wrap(decl);
	}

	public Maybe<Module> findChild(String name)
	{
		Module module = children.get(name);
		return Maybe.wrap(module);
	}

	public Module findOrCreateChild(String name)
	{
		Maybe<Module> child = findChild(name);

		if(child.isSome())
			return child.get();

		Path path = new Path();
		path.components.addAll(this.path.components);
		path.components.add(name);

		Module module = new Module(name, path, this);
		children.put(name, module);

		return module;
	}

	public Map<String, Module> children()
	{
		return children;
	}

	public Map<String, Data> datas()
	{
		return datas;
	}

	public Map<String, Global> globals()
	{
		return globals;
	}

	public Map<String, Function> functions()
	{
		return functions;
	}

	public Map<String, ExternFunction> externFunctions()
	{
		return externFunctions;
	}

	public Path path()
	{
		return path;
	}

	private String name;
	private Path path;
	private Module parent;
	private Map<String, Module> children = new TreeMap<>();

	private Map<String, Decl> decls = new TreeMap<>();
	private Map<String, Data> datas = new TreeMap<>();
	private Map<String, Global> globals = new TreeMap<>();
	private Map<String, Function> functions = new TreeMap<>();
	private Map<String, ExternFunction> externFunctions = new TreeMap<>();
}
