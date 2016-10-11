// Copyright 2016 Markus Grech
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package katana.sema.decl;

import katana.sema.SemaModule;
import katana.sema.type.SemaType;
import katana.utils.Maybe;

import java.util.*;

public class SemaDeclData extends SemaDecl
{
	public class Field
	{
		public Field(String name, SemaType type, int index)
		{
			this.name = name;
			this.type = type;
			this.index = index;
		}

		public SemaDeclData data()
		{
			return SemaDeclData.this;
		}

		public String name;
		public SemaType type;
		public int index;
	}

	public SemaDeclData(SemaModule module, boolean exported, boolean opaque, String name)
	{
		super(module, exported, opaque);
		this.name = name;
	}

	public boolean defineField(String name, SemaType type)
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