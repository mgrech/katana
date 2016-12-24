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

package katana.ast.decl;

import katana.ast.type.AstType;

import java.util.List;

public class AstDeclStruct extends AstDecl
{
	public static class Field
	{
		public Field(AstType type, String name)
		{
			this.type = type;
			this.name = name;
		}

		public AstType type;
		public String name;

		@Override
		public String toString()
		{
			return String.format("%s\n\t\ttype: %s\n\t\tname: %s\n", Field.class.getName(), type, name);
		}
	}

	public AstDeclStruct(boolean exported, boolean opaque, String name, boolean abiCompat, List<Field> fields)
	{
		super(exported, opaque);
		this.name = name;
		this.abiCompat = abiCompat;
		this.fields = fields;
	}

	public boolean abiCompat;
	public String name;
	public List<Field> fields;

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		for(Field field : fields)
			builder.append("\t" + field.toString());

		return String.format("%s\tname: %s\n%s", super.toString(), name, builder);
	}
}
