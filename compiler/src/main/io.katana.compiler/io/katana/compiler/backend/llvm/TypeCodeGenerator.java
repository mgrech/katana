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
import io.katana.compiler.backend.llvm.ir.IrType;
import io.katana.compiler.backend.llvm.ir.IrTypes;
import io.katana.compiler.sema.type.*;
import io.katana.compiler.visitor.IVisitor;

import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class TypeCodeGenerator implements IVisitor
{
	private PlatformContext context;

	private TypeCodeGenerator(PlatformContext context)
	{
		this.context = context;
	}

	public static IrType generate(SemaType type, PlatformContext context)
	{
		var visitor = new TypeCodeGenerator(context);
		return (IrType)type.accept(visitor);
	}

	private IrType lower(SemaType type)
	{
		return (IrType)type.accept(this);
	}

	private IrType visit(SemaTypeBuiltin type)
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
			return IrTypes.ofInteger(Types.sizeof(type, context) * 8);

		case BYTE:
			return IrTypes.I8;

		case BOOL:    return IrTypes.I1;
		case FLOAT32: return IrTypes.FLOAT;
		case FLOAT64: return IrTypes.DOUBLE;
		case VOID:    return IrTypes.VOID;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private IrType visit(SemaTypeFunction type)
	{
		var returnType = lower(type.ret);
		var parameterTypes = type.params.stream()
		                                .map(this::lower)
		                                .collect(Collectors.toList());

		return IrTypes.ofFunction(returnType, parameterTypes);
	}

	private IrType visit(SemaTypeStruct type)
	{
		return IrTypes.ofIdentifiedStruct(type.decl.qualifiedName().toString());
	}

	private IrType visit(SemaTypeSlice type)
	{
		var elementType = lower(type.type);
		var elementPointerType = IrTypes.ofPointer(elementType);
		var lengthType = lower(SemaTypeBuiltin.INT);
		return IrTypes.ofLiteralStruct(elementPointerType, lengthType);
	}

	private IrType visit(SemaTypeArray type)
	{
		return IrTypes.ofArray(type.length, lower(type.type));
	}

	private IrType visit(SemaTypeConst type)
	{
		return lower(type.type);
	}

	private IrType visit(SemaTypeNullablePointer type)
	{
		if(Types.isZeroSized(type.type))
			return IrTypes.ofPointer(IrTypes.I8);

		return IrTypes.ofPointer(lower(type.type));
	}

	private IrType visit(SemaTypeNonNullablePointer type)
	{
		if(Types.isZeroSized(type.type))
			return IrTypes.ofPointer(IrTypes.I8);

		return IrTypes.ofPointer(lower(type.type));
	}

	private IrType visit(SemaTypeTuple tuple)
	{
		return IrTypes.ofLiteralStruct(tuple.types.stream()
		                                          .map(this::lower)
		                                          .collect(Collectors.toList()));
	}
}
