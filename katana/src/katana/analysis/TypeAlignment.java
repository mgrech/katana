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

import katana.BuiltinType;
import katana.backend.PlatformContext;
import katana.diag.CompileException;
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
		if(builtinType.which == BuiltinType.VOID)
			throw new CompileException("'alignof' applied to void type");

		return TypeSize.of(builtinType, context);
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

	private BigInteger visit(SemaTypePointer pointerType)
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
