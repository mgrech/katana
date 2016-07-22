package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Stmt;
import katana.ast.Type;

import java.util.ArrayList;

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

	public Function(boolean exported, String name, ArrayList<Parameter> params, ArrayList<Stmt> body)
	{
		this.exported = exported;
		this.name = name;
		this.params = params;
		this.body = body;
	}

	public boolean exported;
	public String name;
	public ArrayList<Parameter> params;
	public ArrayList<Stmt> body;
}
