// Copyright 2016-2019 Markus Grech
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

package io.katana.compiler.ast.decl;

import io.katana.compiler.ExportKind;
import io.katana.compiler.ast.type.AstType;

import java.util.List;

public class AstDeclStruct extends AstDecl
{
	public static class Field
	{
		public AstType type;
		public String name;

		public Field(AstType type, String name)
		{
			this.type = type;
			this.name = name;
		}

		@Override
		public String toString()
		{
			return String.format("%s\n\t\ttype: %s\n\t\tname: %s\n", Field.class.getName(), type, name);
		}
	}

	public boolean abiCompatible;
	public String name;
	public List<Field> fields;

	public AstDeclStruct(ExportKind exportKind, String name, boolean abiCompatible, List<Field> fields)
	{
		super(exportKind);
		this.name = name;
		this.abiCompatible = abiCompatible;
		this.fields = fields;
	}

	@Override
	public String toString()
	{
		var builder = new StringBuilder();

		for(var field : fields)
			builder.append("\t" + field.toString());

		return String.format("%s\tname: %s\n%s", super.toString(), name, builder);
	}
}
