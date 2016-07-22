package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Type;

import java.util.ArrayList;

public class Data extends Decl
{
	public static class Field
	{
		public String name;
		public Type type;
	}

	public boolean exported;
	public boolean opaque;
	public String name;
	public ArrayList<Field> fields;
}
