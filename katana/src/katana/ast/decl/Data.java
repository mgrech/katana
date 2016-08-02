package katana.ast.decl;

import katana.ast.Decl;
import katana.ast.Type;

import java.util.ArrayList;

public class Data extends Decl
{
	public static class Field
	{
		public Field(Type type, String name)
		{
			this.type = type;
			this.name = name;
		}

		public Type type;
		public String name;

		@Override
		public String toString()
		{
			return String.format("%s\n\t\ttype: %s\n\t\tname: %s\n", Field.class.getName(), type, name);
		}
	}

	public Data(boolean exported, boolean opaque, String name, ArrayList<Field> fields)
	{
		super(exported, opaque);
		this.name = name;
		this.fields = fields;
	}

	public String name;
	public ArrayList<Field> fields;

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		for(Field field : fields)
			builder.append("\t" + field.toString());

		return String.format("%s\tname: %s\n%s", super.toString(), name, builder);
	}
}
