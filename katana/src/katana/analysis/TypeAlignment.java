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
import katana.platform.Arch;
import katana.sema.type.*;
import katana.visitor.IVisitor;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class TypeAlignment implements IVisitor
{
	private final PlatformContext context;

	private TypeAlignment(PlatformContext context)
	{
		this.context = context;
	}

	private BigInteger visit(SemaTypeArray arrayType)
	{
		return TypeAlignment.of(arrayType.type, context);
	}

	private BigInteger visit(SemaTypeBuiltin builtinType)
	{
		Arch arch = context.target().arch;

		switch(builtinType.which)
		{
		case VOID: return BigInteger.ONE;
		case BYTE: return BigInteger.ONE;
		case BOOL: return BigInteger.ONE;
		case INT8:  case UINT8:  return BigInteger.ONE;
		case INT16: case UINT16: return arch.int16Align;
		case INT32: case UINT32: return arch.int32Align;
		case INT64: case UINT64: return arch.int64Align;
		case INT:   case UINT:   return arch.pointerAlign;
		case FLOAT32: return arch.float32Align;
		case FLOAT64: return arch.float64Align;
		default: throw new AssertionError("unreachable");
		}
	}

	private BigInteger visit(SemaTypeConst constType)
	{
		return of(constType.type, context);
	}

	private BigInteger visit(SemaTypeFunction functionType)
	{
		throw new CompileException("'alignof' applied to function type");
	}

	private BigInteger visit(SemaTypeNullablePointer pointerType)
	{
		return TypeSize.of(pointerType, context);
	}

	private BigInteger visit(SemaTypeOpaque opaqueType)
	{
		return opaqueType.alignment;
	}

	private BigInteger visit(SemaTypeNonNullablePointer pointerType)
	{
		return TypeSize.of(pointerType, context);
	}

	private BigInteger visit(SemaTypeUserDefined userDefinedType)
	{
		return userDefinedType.decl.layout.alignof();
	}

	public static BigInteger of(SemaType type, PlatformContext context)
	{
		return (BigInteger)type.accept(new TypeAlignment(context));
	}
}
