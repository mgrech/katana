// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.sema.decl;

import io.katana.compiler.ExportKind;
import io.katana.compiler.analysis.StructLayout;
import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.utils.Maybe;

import java.util.*;

public class SemaDeclStruct extends SemaDecl
{
	public class Field
	{
		public String name;
		public SemaType type;
		public int index;

		public Field(String name, SemaType type, int index)
		{
			this.name = name;
			this.type = type;
			this.index = index;
		}

		public long offsetof()
		{
			return struct().layout.offsetof(index);
		}

		public SemaDeclStruct struct()
		{
			return SemaDeclStruct.this;
		}
	}

	private String name;
	public boolean abiCompatible;
	private final List<Field> fields = new ArrayList<>();
	private final Map<String, Field> fieldsByName = new TreeMap<>();
	public StructLayout layout;

	public SemaDeclStruct(SemaModule module, ExportKind exportKind, String name, boolean abiCompatible)
	{
		super(module, exportKind);
		this.name = name;
		this.abiCompatible = abiCompatible;
	}

	public boolean defineField(String name, SemaType type)
	{
		if(fieldsByName.containsKey(name))
			return false;

		var field = new Field(name, type, fields.size());
		fields.add(field);
		fieldsByName.put(name, field);
		return true;
	}

	public Maybe<Field> findField(String name)
	{
		var field = fieldsByName.get(name);
		return Maybe.wrap(field);
	}

	public List<Field> fieldsByIndex()
	{
		return Collections.unmodifiableList(fields);
	}

	@Override
	public String name()
	{
		return name;
	}
}
