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

package io.katana.compiler.diag;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.type.*;
import io.katana.compiler.visitor.IVisitor;

@SuppressWarnings("unused")
public class TypeString extends IVisitor<String>
{
	private static final TypeString INSTANCE = new TypeString();

	private TypeString() {}

	public static String of(SemaType type)
	{
		return INSTANCE.invokeSelf(type);
	}

	private String visit(SemaTypeStruct type)
	{
		return type.decl.qualifiedName().toString();
	}

	private String visit(SemaTypeConst type)
	{
		return String.format("const %s", of(type.nestedType));
	}

	private String visit(SemaTypeSlice type)
	{
		return String.format("[]%s", of(type.elementType));
	}

	private String visit(SemaTypeArray type)
	{
		return String.format("[%s]%s", type.length, of(type.elementType));
	}

	private String visit(SemaTypeBuiltin type)
	{
		if(type.which == BuiltinType.NULL)
			return "<null-type>";

		return type.which.toString().toLowerCase();
	}

	private String visit(SemaTypeFunction type)
	{
		var params = new StringBuilder();

		if(!type.paramTypes.isEmpty())
		{
			params.append(of(type.paramTypes.get(0)));

			for(var i = 1; i != type.paramTypes.size(); ++i)
			{
				params.append(", ");
				params.append(of(type.paramTypes.get(i)));
			}
		}

		var ret = Types.isVoid(type.returnType) ? "" : " => " + of(type.returnType);
		return String.format("fn(%s)%s", params, ret);
	}

	private String visit(SemaTypeNullablePointer type)
	{
		return String.format("?%s", of(type.pointeeType));
	}

	private String visit(SemaTypeNonNullablePointer type)
	{
		return String.format("!%s", of(type.pointeeType));
	}
}
