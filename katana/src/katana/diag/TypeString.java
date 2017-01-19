// Copyright 2016-2017 Markus Grech
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

package katana.diag;

import katana.BuiltinType;
import katana.analysis.TypeHelper;
import katana.sema.type.*;
import katana.visitor.IVisitor;

@SuppressWarnings("unused")
public class TypeString implements IVisitor
{
	private static final TypeString INSTANCE = new TypeString();

	private TypeString() {}

	public static String of(SemaType type)
	{
		return (String)type.accept(INSTANCE);
	}

	private String visit(SemaTypeUserDefined type)
	{
		return type.decl.qualifiedName().toString();
	}

	private String visit(SemaTypeConst type)
	{
		return String.format("const %s", of(type.type));
	}

	private String visit(SemaTypeArray type)
	{
		return String.format("[%s]%s", type.length, of(type.type));
	}

	private String visit(SemaTypeOpaque type)
	{
		return String.format("opaque(%s, %s)", type.size, type.alignment);
	}

	private String visit(SemaTypeBuiltin type)
	{
		if(type.which == BuiltinType.NULL)
			return "<null-type>";

		return type.which.toString().toLowerCase();
	}

	private String visit(SemaTypeFunction type)
	{
		StringBuilder params = new StringBuilder();

		if(!type.params.isEmpty())
		{
			params.append(of(type.params.get(0)));

			for(int i = 1; i != type.params.size(); ++i)
			{
				params.append(", ");
				params.append(of(type.params.get(i)));
			}
		}

		String ret = TypeHelper.isVoidType(type.ret) ? "" : " => " + of(type.ret);
		return String.format("fn(%s)%s", params, ret);
	}

	private String visit(SemaTypeNullablePointer type)
	{
		return String.format("?%s", of(type.type));
	}

	private String visit(SemaTypePointer type)
	{
		return String.format("!%s", of(type.type));
	}
}
