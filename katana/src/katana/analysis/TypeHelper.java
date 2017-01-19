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
import katana.sema.type.*;

public class TypeHelper
{
	public static boolean isConst(SemaType type)
	{
		if(type instanceof SemaTypeArray)
			return isConst(((SemaTypeArray)type).type);

		return type instanceof SemaTypeConst;
	}

	public static SemaType addConst(SemaType type)
	{
		if(type instanceof SemaTypeFunction)
			throw new AssertionError("const added to function type");

		if(isConst(type))
			return type;

		if(type instanceof SemaTypeArray)
		{
			SemaTypeArray array = (SemaTypeArray)type;
			return new SemaTypeArray(array.length, addConst(array.type));
		}

		return new SemaTypeConst(type);
	}

	public static SemaType removePointer(SemaType type)
	{
		if(type instanceof SemaTypeNullablePointer)
			return ((SemaTypeNullablePointer)type).type;

		if(type instanceof SemaTypePointer)
			return ((SemaTypePointer)type).type;

		return type;
	}

	public static SemaType removeConst(SemaType type)
	{
		if(type instanceof SemaTypeConst)
			return ((SemaTypeConst)type).type;

		if(type instanceof SemaTypeArray)
		{
			SemaTypeArray array = (SemaTypeArray)type;
			return new SemaTypeArray(array.length, removeConst(array.type));
		}

		return type;
	}

	public static boolean isVoidType(SemaType type)
	{
		return isBuiltinType(type, BuiltinType.VOID);
	}

	public static boolean isBuiltinType(SemaType type, BuiltinType which)
	{
		type = removeConst(type);

		if(!(type instanceof SemaTypeBuiltin))
			return false;

		return ((SemaTypeBuiltin)type).which == which;
	}

	public static boolean isBuiltinKind(SemaType type, BuiltinType.Kind kind)
	{
		type = removeConst(type);

		if(!(type instanceof SemaTypeBuiltin))
			return false;

		return ((SemaTypeBuiltin)type).which.kind == kind;
	}

	public static boolean isSigned(SemaType type)
	{
		return isBuiltinKind(type, BuiltinType.Kind.INT);
	}

	public static boolean isUnsigned(SemaType type)
	{
		return isBuiltinKind(type, BuiltinType.Kind.UINT);
	}

	public static boolean isIntegerType(SemaType type)
	{
		return isSigned(type) || isUnsigned(type);
	}

	public static boolean isFloatingPointType(SemaType type)
	{
		return isBuiltinKind(type, BuiltinType.Kind.FLOAT);
	}

	public static boolean isArrayType(SemaType type)
	{
		return type instanceof SemaTypeArray;
	}

	public static boolean isFunctionType(SemaType type)
	{
		return type instanceof SemaTypeFunction;
	}

	public static boolean isPointerType(SemaType type)
	{
		type = removeConst(type);
		return type instanceof SemaTypePointer || type instanceof SemaTypeNullablePointer;
	}

	public static boolean equalSizes(SemaType first, SemaType second, PlatformContext context)
	{
		return TypeSize.of(first, context).equals(TypeSize.of(second, context));
	}

	public static int compareSizes(SemaType first, SemaType second, PlatformContext context)
	{
		return TypeSize.of(first, context).compareTo(TypeSize.of(second, context));
	}
}
