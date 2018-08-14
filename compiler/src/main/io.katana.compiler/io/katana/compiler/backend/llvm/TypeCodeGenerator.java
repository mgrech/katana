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

package io.katana.compiler.backend.llvm;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.sema.type.*;
import io.katana.compiler.visitor.IVisitor;

@SuppressWarnings("unused")
public class TypeCodeGenerator implements IVisitor
{
	private PlatformContext context;

	private TypeCodeGenerator(PlatformContext context)
	{
		this.context = context;
	}

	public static String generate(SemaType type, PlatformContext context)
	{
		TypeCodeGenerator visitor = new TypeCodeGenerator(context);
		return (String)type.accept(visitor);
	}

	private String visit(SemaTypeBuiltin type)
	{
		switch(type.which)
		{
		case INT8:
		case INT16:
		case INT32:
		case INT64:
		case INT:
		case UINT8:
		case UINT16:
		case UINT32:
		case UINT64:
		case UINT:
			return "i" + Types.sizeof(type, context) * 8;

		case BYTE:
			return "i8";

		case BOOL:    return "i1";
		case FLOAT32: return "float";
		case FLOAT64: return "double";
		case VOID:    return "void";

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private String visit(SemaTypeFunction type)
	{
		String ret = TypeCodeGenerator.generate(type.ret, context);

		StringBuilder params = new StringBuilder();

		if(!type.params.isEmpty())
		{
			params.append(TypeCodeGenerator.generate(type.params.get(0), context));

			for(int i = 1; i != type.params.size(); ++i)
			{
				params.append(", ");
				params.append(TypeCodeGenerator.generate(type.params.get(i), context));
			}
		}

		return String.format("%s(%s)", ret, params.toString());
	}

	private String visit(SemaTypeStruct type)
	{
		return '%' + type.decl.qualifiedName().toString();
	}

	private String visit(SemaTypeSlice type)
	{
		var baseTypeString = TypeCodeGenerator.generate(type.type, context);
		var intString = TypeCodeGenerator.generate(SemaTypeBuiltin.INT, context);
		return String.format("{%s*, %s}", baseTypeString, intString);
	}

	private String visit(SemaTypeArray type)
	{
		return String.format("[%s x %s]", type.length, TypeCodeGenerator.generate(type.type, context));
	}

	private String visit(SemaTypeConst type)
	{
		return generate(type.type, context);
	}

	private String visit(SemaTypeNullablePointer type)
	{
		if(Types.isZeroSized(type.type))
			return "i8*";

		return String.format("%s*", generate(type.type, context));
	}

	private String visit(SemaTypeNonNullablePointer type)
	{
		if(Types.isZeroSized(type.type))
			return "i8*";

		return String.format("%s*", generate(type.type, context));
	}

	private String visit(SemaTypeTuple tuple)
	{
		StringBuilder builder = new StringBuilder();
		builder.append('{');

		if(!tuple.types.isEmpty())
		{
			builder.append(generate(tuple.types.get(0), context));

			for(int i = 1; i != tuple.types.size(); ++i)
			{
				builder.append(", ");
				builder.append(generate(tuple.types.get(i), context));
			}
		}

		builder.append('}');

		return builder.toString();
	}
}
