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
