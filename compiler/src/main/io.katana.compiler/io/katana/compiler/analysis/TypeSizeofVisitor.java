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

package io.katana.compiler.analysis;

import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.sema.type.*;
import io.katana.compiler.visitor.IVisitor;

@SuppressWarnings("unused")
public class TypeSizeofVisitor implements IVisitor
{
	private final PlatformContext context;

	private TypeSizeofVisitor(PlatformContext context)
	{
		this.context = context;
	}

	private long visit(SemaTypeArray arrayType)
	{
		return arrayType.length * apply(arrayType.type, context);
	}

	private long visit(SemaTypeBuiltin builtinType)
	{
		switch(builtinType.which)
		{
		case VOID:
		case NULL:
			return 0;

		case BYTE:
		case BOOL:
		case INT8:
		case UINT8:
			return 1;

		case INT16:
		case UINT16:
			return 2;

		case INT32:
		case UINT32:
		case FLOAT32:
			return 4;

		case INT64:
		case UINT64:
		case FLOAT64:
			return 8;

		case INT:
		case UINT:
			return context.target().arch.pointerSize;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private long visit(SemaTypeConst constType)
	{
		return apply(constType.type, context);
	}

	private long visit(SemaTypeFunction functionType)
	{
		throw new CompileException("'sizeof' applied to function type");
	}

	private long visit(SemaTypeNullablePointer pointerType)
	{
		return context.target().arch.pointerSize;
	}

	private long visit(SemaTypeNonNullablePointer pointerType)
	{
		return context.target().arch.pointerSize;
	}

	private long visit(SemaTypeTuple tuple)
	{
		return tuple.layout.sizeof();
	}

	private long visit(SemaTypeStruct userDefinedType)
	{
		return userDefinedType.decl.layout.sizeof();
	}

	public static long apply(SemaType type, PlatformContext context)
	{
		return (long)type.accept(new TypeSizeofVisitor(context));
	}
}
