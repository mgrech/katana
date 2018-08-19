// Copyright 2018 Markus Grech
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

package io.katana.compiler.backend.llvm.ir.type;

import java.util.*;

public class IrTypes
{
	private static final Map<String, IrTypeScalar> SCALAR_CACHE = new HashMap<>();
	private static final Map<IrType, IrTypePointer> POINTER_CACHE = new IdentityHashMap<>();
	private static final Map<String, IrTypeStructIdentified> IDENTIFIED_STRUCT_CACHE = new HashMap<>();

	public static final IrTypeScalar VOID = ofScalar("void");

	public static final IrTypeScalar I1  = ofInteger(1);
	public static final IrTypeScalar I8  = ofInteger(8);
	public static final IrTypeScalar I16 = ofInteger(16);
	public static final IrTypeScalar I32 = ofInteger(32);
	public static final IrTypeScalar I64 = ofInteger(64);

	public static final IrTypeScalar FLOAT  = ofScalar("float");
	public static final IrTypeScalar DOUBLE = ofScalar("double");

	public static IrTypeScalar ofScalar(String name)
	{
		return SCALAR_CACHE.computeIfAbsent(name, IrTypeScalar::new);
	}

	public static IrTypeScalar ofInteger(long width)
	{
		return ofScalar("i" + width);
	}

	public static IrTypeScalar ofFloat(long width)
	{
		switch((int)width)
		{
		case 32: return FLOAT;
		case 64: return DOUBLE;
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	public static IrTypePointer ofPointer(IrType pointeeType)
	{
		return POINTER_CACHE.computeIfAbsent(pointeeType, IrTypePointer::new);
	}

	public static IrTypeArray ofArray(long length, IrType elementType)
	{
		return new IrTypeArray(length, elementType);
	}

	public static IrTypeStructIdentified ofIdentifiedStruct(String name)
	{
		return IDENTIFIED_STRUCT_CACHE.computeIfAbsent(name, IrTypeStructIdentified::new);
	}

	public static IrTypeStructLiteral ofLiteralStruct(IrType... fields)
	{
		return ofLiteralStruct(Arrays.asList(fields));
	}

	public static IrTypeStructLiteral ofLiteralStruct(List<IrType> fields)
	{
		return new IrTypeStructLiteral(fields);
	}

	public static IrTypeFunction ofFunction(IrType returnType, List<IrType> parameterTypes)
	{
		return new IrTypeFunction(returnType, parameterTypes);
	}
}
