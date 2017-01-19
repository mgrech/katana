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

package katana.analysis;

import katana.backend.PlatformContext;
import katana.diag.CompileException;
import katana.sema.type.*;
import katana.visitor.IVisitor;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class TypeSize implements IVisitor
{
	private static final BigInteger TWO = BigInteger.valueOf(2);
	private static final BigInteger FOUR = BigInteger.valueOf(4);
	private static final BigInteger EIGHT = BigInteger.valueOf(8);

	private final PlatformContext context;

	private TypeSize(PlatformContext context)
	{
		this.context = context;
	}

	private BigInteger visit(SemaTypeArray arrayType)
	{
		return arrayType.length.multiply(of(arrayType.type, context));
	}

	private BigInteger visit(SemaTypeBuiltin builtinType)
	{
		switch(builtinType.which)
		{
		case VOID:
			throw new CompileException("'sizeof' applied to void type");

		case BOOL:
		case INT8:
		case UINT8:
			return BigInteger.ONE;

		case INT16:
		case UINT16:
			return TWO;

		case INT32:
		case UINT32:
		case FLOAT32:
			return FOUR;

		case INT64:
		case UINT64:
		case FLOAT64:
			return EIGHT;

		case INT:
		case UINT:
			return context.target().arch.intSize;

		case PINT:
		case UPINT:
		case NULL:
			return context.target().arch.pointerSize;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private BigInteger visit(SemaTypeConst constType)
	{
		return of(constType.type, context);
	}

	private BigInteger visit(SemaTypeFunction functionType)
	{
		throw new CompileException("'sizeof' applied to function type");
	}

	private BigInteger visit(SemaTypeNullablePointer pointerType)
	{
		return context.target().arch.pointerSize;
	}

	private BigInteger visit(SemaTypeOpaque opaqueType)
	{
		return opaqueType.size;
	}

	private BigInteger visit(SemaTypePointer pointerType)
	{
		return context.target().arch.pointerSize;
	}

	private BigInteger visit(SemaTypeUserDefined userDefinedType)
	{
		return userDefinedType.decl.layout.sizeof();
	}

	public static BigInteger of(SemaType type, PlatformContext context)
	{
		return (BigInteger)type.accept(new TypeSize(context));
	}
}
