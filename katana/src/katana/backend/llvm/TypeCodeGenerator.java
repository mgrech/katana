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

package katana.backend.llvm;

import katana.backend.PlatformContext;
import katana.sema.type.*;
import katana.visitor.IVisitor;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class TypeCodeGenerator implements IVisitor
{
	private PlatformContext context;

	private TypeCodeGenerator(PlatformContext context)
	{
		this.context = context;
	}

	public static String generate(Type type, PlatformContext context)
	{
		TypeCodeGenerator visitor = new TypeCodeGenerator(context);
		return (String)type.accept(visitor);
	}

	private String visit(Builtin type)
	{
		switch(type.which)
		{
		case INT8:
		case UINT8:
		case INT16:
		case UINT16:
		case INT32:
		case UINT32:
		case INT64:
		case UINT64:
		case INT:
		case UINT:
		case PINT:
		case UPINT:
			return "i" + type.sizeof(context).multiply(BigInteger.valueOf(8));

		case BOOL:    return "i1";
		case FLOAT32: return "float";
		case FLOAT64: return "double";
		case PTR:     return "i8*";

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private String visit(Opaque type)
	{
		return String.format("[%s x i8]", type.sizeof(context));
	}

	private String visit(Function type)
	{
		String ret = type.ret.isSome() ? TypeCodeGenerator.generate(type.ret.get(), context) : "void";

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

	private String visit(UserDefined type)
	{
		return '%' + type.data.qualifiedName().toString();
	}

	private String visit(Array type)
	{
		return String.format("[%s x %s]", type.length, TypeCodeGenerator.generate(type.type, context));
	}
}
