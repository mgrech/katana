package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Stmt;
import katana.ast.Type;

import java.util.ArrayList;
import java.util.Optional;

public class Function extends Decl
{
	public static class Parameter
	{
		public Parameter(Type type, String name)
		{
			this.type = type;
			this.name = name;
		}

		public Type type;
		public String name;
	}

	public static class Local
	{
		public Local(Type type, String name)
		{
			this.type = type;
			this.name = name;
		}

		public Type type;
		public String name;
	}

	public Function(boolean exported, boolean opaque, String name, ArrayList<Parameter> params, Optional<Type> ret, ArrayList<Local> locals, ArrayList<Stmt> body)
	{
		super(exported, opaque);
		this.name = name;
		this.params = params;
		this.ret = ret;
		this.locals = locals;
		this.body = body;
	}

	public String name;
	public ArrayList<Parameter> params;
	public Optional<Type> ret;
	public ArrayList<Local> locals;
	public ArrayList<Stmt> body;

	@Override
	public String toString()
	{
		StringBuilder params = new StringBuilder();

		for(Parameter param : this.params)
		{
			params.append("\t\t");
			params.append(param.name);
			params.append(" (");
			params.append(param.type);
			params.append(")\n");
		}

		String ret = this.ret.isPresent() ? "\tret: " + this.ret.get() + '\n' : "";

		StringBuilder locals = new StringBuilder();

		for(Local local : this.locals)
		{
			locals.append("\t\t");
			locals.append(local.name);
			locals.append(" (");
			locals.append(local.type);
			locals.append(")\n");
		}

		StringBuilder body = new StringBuilder();

		for(Stmt stmt : this.body)
		{
			body.append("\t\t");
			body.append(stmt);
			body.append('\n');
		}

		return String.format("%sname: %s\n\tparams:\n%s%s\tlocals:\n%s\tbody:\n%s", super.toString(), name, params, ret, locals, body);
	}
}
