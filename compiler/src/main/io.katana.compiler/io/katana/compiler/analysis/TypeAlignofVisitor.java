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
public class TypeAlignofVisitor extends IVisitor<Long>
{
	private final PlatformContext context;

	private TypeAlignofVisitor(PlatformContext context)
	{
		this.context = context;
	}

	private long visit(SemaTypeSlice sliceType)
	{
		return Types.sliceLayout(context).alignof();
	}

	private long visit(SemaTypeArray arrayType)
	{
		return apply(arrayType.elementType, context);
	}

	private long visit(SemaTypeBuiltin builtinType)
	{
		var arch = context.target().arch;

		switch(builtinType.which)
		{
		case VOID:
		case BYTE:
		case BOOL:
		case INT8:  case UINT8:
			return 1;

		case INT16: case UINT16: return arch.int16Align;
		case INT32: case UINT32: return arch.int32Align;
		case INT64: case UINT64: return arch.int64Align;
		case INT:   case UINT:   return arch.pointerAlign;
		case FLOAT32: return arch.float32Align;
		case FLOAT64: return arch.float64Align;
		default: throw new AssertionError("unreachable");
		}
	}

	private long visit(SemaTypeConst constType)
	{
		return apply(constType.nestedType, context);
	}

	private long visit(SemaTypeFunction functionType)
	{
		throw new CompileException("'alignof' applied to function type");
	}

	private long visit(SemaTypeNullablePointer pointerType)
	{
		return context.target().arch.pointerAlign;
	}

	private long visit(SemaTypeNonNullablePointer pointerType)
	{
		return context.target().arch.pointerAlign;
	}

	private long visit(SemaTypeTuple tuple)
	{
		return tuple.layout.alignof();
	}

	private long visit(SemaTypeStruct userDefinedType)
	{
		return userDefinedType.decl.layout.alignof();
	}

	public static long apply(SemaType type, PlatformContext context)
	{
		return new TypeAlignofVisitor(context).invokeSelf(type);
	}
}
