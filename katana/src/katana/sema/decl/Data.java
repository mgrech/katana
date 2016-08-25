package katana.sema.decl;

import katana.Maybe;
import katana.sema.Decl;
import katana.sema.Module;
import katana.sema.Type;

import java.util.*;

public class Data extends Decl
{
	public class Field
	{
		public Field(String name, Type type, int index)
		{
			this.name = name;
			this.type = type;
			this.index = index;
		}

		public String name;
		public Type type;
		public int index;
	}

	public Data(Module module, String name)
	{
		super(module);
		this.name = name;
	}

	public boolean defineField(String name, Type type)
	{
		if(fieldsByName.containsKey(name))
			return false;

		Field field = new Field(name, type, fields.size());
		fields.add(field);
		fieldsByName.put(name, field);
		return true;
	}

	public Maybe<Field> findField(String name)
	{
		Field field = fieldsByName.get(name);
		return Maybe.wrap(field);
	}

	public List<Field> fieldsByIndex()
	{
		return Collections.unmodifiableList(fields);
	}

	public Map<String, Field> fieldsByName()
	{
		return Collections.unmodifiableMap(fieldsByName);
	}

	@Override
	public String name()
	{
		return name;
	}

	private String name;
	private final List<Field> fields = new ArrayList<>();
	private final Map<String, Field> fieldsByName = new TreeMap<>();
}
