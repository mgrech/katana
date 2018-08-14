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

import io.katana.compiler.BuiltinType;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.sema.decl.SemaDeclStruct;
import io.katana.compiler.sema.type.*;

public class Types
{
	public static boolean equal(SemaType left, SemaType right)
	{
		return TypesEqualVisitor.apply(left, right);
	}

	public static boolean isConst(SemaType type)
	{
		if(type instanceof SemaTypeArray)
			return isConst(((SemaTypeArray)type).type);

		return type instanceof SemaTypeConst;
	}

	public static SemaType addNullablePointer(SemaType pointeeType)
	{
		return new SemaTypeNullablePointer(pointeeType);
	}

	public static SemaType addNonNullablePointer(SemaType pointeeType)
	{
		return new SemaTypeNonNullablePointer(pointeeType);
	}

	public static SemaType copyPointerKind(SemaType sourceType, SemaType pointeeType)
	{
		if(isNullablePointer(sourceType))
			return addNullablePointer(pointeeType);

		if(isNonNullablePointer(sourceType))
			return addNonNullablePointer(pointeeType);

		throw new AssertionError("unreachable");
	}

	public static long arrayLength(SemaType type)
	{
		return ((SemaTypeArray)type).length;
	}

	public static SemaType addConst(SemaType type)
	{
		if(type instanceof SemaTypeFunction)
			return type;

		if(isConst(type))
			return type;

		if(type instanceof SemaTypeArray)
		{
			SemaTypeArray array = (SemaTypeArray)type;
			return new SemaTypeArray(array.length, addConst(array.type));
		}

		return new SemaTypeConst(type);
	}

	public static SemaType addSlice(SemaType type)
	{
		return new SemaTypeSlice(type);
	}

	public static SemaType removeSlice(SemaType type)
	{
		if(type instanceof SemaTypeSlice)
			return ((SemaTypeSlice)type).type;

		return type;
	}

	public static SemaType removeArray(SemaType type)
	{
		if(type instanceof SemaTypeArray)
			return ((SemaTypeArray)type).type;

		return type;
	}

	public static SemaType removePointer(SemaType type)
	{
		type = removeConst(type);

		if(type instanceof SemaTypeNullablePointer)
			return ((SemaTypeNullablePointer)type).type;

		if(type instanceof SemaTypeNonNullablePointer)
			return ((SemaTypeNonNullablePointer)type).type;

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

	public static boolean isVoid(SemaType type)
	{
		return isBuiltin(type, BuiltinType.VOID);
	}

	public static boolean isByte(SemaType type)
	{
		return isBuiltin(type, BuiltinType.BYTE);
	}

	public static boolean isBuiltin(SemaType type, BuiltinType which)
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

	public static boolean isInteger(SemaType type)
	{
		return isSigned(type) || isUnsigned(type);
	}

	public static boolean isFixedSizeInteger(SemaType type)
	{
		return isInteger(type) && !isBuiltin(type, BuiltinType.INT) && !isBuiltin(type, BuiltinType.UINT);
	}

	public static boolean isFloatingPoint(SemaType type)
	{
		return isBuiltinKind(type, BuiltinType.Kind.FLOAT);
	}

	public static boolean isSlice(SemaType type)
	{
		return type instanceof SemaTypeSlice;
	}

	public static boolean isArray(SemaType type)
	{
		return type instanceof SemaTypeArray;
	}

	public static boolean isFunction(SemaType type)
	{
		return type instanceof SemaTypeFunction;
	}

	public static boolean isNullablePointer(SemaType type)
	{
		type = removeConst(type);
		return type instanceof SemaTypeNullablePointer;
	}

	public static boolean isNonNullablePointer(SemaType type)
	{
		type = removeConst(type);
		return type instanceof SemaTypeNonNullablePointer;
	}

	public static boolean isPointer(SemaType type)
	{
		return isNullablePointer(type) || isNonNullablePointer(type);
	}

	public static boolean equalSizes(SemaType first, SemaType second, PlatformContext context)
	{
		return sizeof(first, context) == sizeof(second, context);
	}

	public static int compareSizes(SemaType first, SemaType second, PlatformContext context)
	{
		return Long.compare(sizeof(first, context), sizeof(second, context));
	}

	public static boolean isZeroSized(SemaDeclStruct struct)
	{
		return struct.layout.sizeof() == 0;
	}

	public static boolean isZeroSized(SemaType type)
	{
		return CheckZeroSizeVisitor.apply(type);
	}

	public static long sizeof(SemaType type, PlatformContext context)
	{
		return TypeSizeofVisitor.apply(type, context);
	}

	public static long alignof(SemaType type, PlatformContext context)
	{
		return TypeAlignofVisitor.apply(type, context);
	}

	public static StructLayout sliceLayout(PlatformContext context)
	{
		var voidptr = new SemaTypeNullablePointer(SemaTypeBuiltin.VOID);

		return new StructLayoutBuilder(context).appendField(voidptr)
		                                       .appendField(SemaTypeBuiltin.INT)
		                                       .build();
	}
}
