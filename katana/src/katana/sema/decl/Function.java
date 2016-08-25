package katana.sema.decl;

import katana.Maybe;
import katana.sema.Decl;
import katana.sema.Module;
import katana.sema.Stmt;
import katana.sema.Type;
import katana.sema.stmt.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Function extends Decl
{
	public class Param
	{
		public Param(String name, Type type, int index)
		{
			this.name = name;
			this.type = type;
			this.index = index;
		}

		public String name;
		public Type type;
		public int index;
	}

	public class Local
	{
		public Local(String name, Type type, int index)
		{
			this.name = name;
			this.type = type;
			this.index = index;
		}

		public String name;
		public Type type;
		public int index;
	}

	public Function(Module module, String name, Maybe<Type> ret)
	{
		super(module);
		this.name = name;
		this.ret = ret;
	}

	public boolean defineParam(String name, Type type)
	{
		if(paramsByName.containsKey(name))
			return false;

		Param param = new Param(name, type, params.size());
		params.add(param);
		paramsByName.put(name, param);
		return true;
	}

	public boolean defineLocal(String name, Type type)
	{
		if(localsByName.containsKey(name))
			return false;

		Local local = new Local(name, type, locals.size());
		locals.add(local);
		localsByName.put(name, local);
		return true;
	}

	public boolean defineLabel(Label label)
	{
		if(labels.containsKey(label.name))
			return false;

		labels.put(label.name, label);
		return true;
	}

	public Maybe<Param> findParam(String name)
	{
		Param param = paramsByName.get(name);
		return Maybe.wrap(param);
	}

	public Maybe<Local> findLocal(String name)
	{
		Local local = localsByName.get(name);
		return Maybe.wrap(local);
	}

	public void add(Stmt stmt)
	{
		body.add(stmt);
	}

	@Override
	public String name()
	{
		return name;
	}

	private String name;
	public List<Param> params = new ArrayList<>();
	public Map<String, Param> paramsByName = new TreeMap<>();
	public Maybe<Type> ret;

	public List<Local> locals = new ArrayList<>();
	public Map<String, Local> localsByName = new TreeMap<>();
	public List<Stmt> body = new ArrayList<>();
	public Map<String, Label> labels = new TreeMap<>();
}
