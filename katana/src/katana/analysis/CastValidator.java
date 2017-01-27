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
import katana.sema.expr.SemaExprCast;
import katana.sema.type.SemaType;

public class CastValidator
{
	private static boolean isValidSignCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		if(!TypeHelper.isIntegerType(sourceType) || !TypeHelper.isIntegerType(targetType))
			return false;

		return TypeHelper.equalSizes(sourceType, targetType, context);
	}

	private static boolean isOneOf(SemaType type, BuiltinType... builtinTypes)
	{
		for(BuiltinType builtinType : builtinTypes)
			if(TypeHelper.isBuiltinType(type, builtinType))
				return true;

		return false;
	}

	private static boolean isValidWidenCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		if(isOneOf(sourceType, BuiltinType.INT, BuiltinType.UINT, BuiltinType.PINT, BuiltinType.UPINT)
		|| isOneOf(targetType, BuiltinType.INT, BuiltinType.UINT, BuiltinType.PINT, BuiltinType.UPINT))
			return false;

		if(TypeHelper.isIntegerType(sourceType) && TypeHelper.isIntegerType(targetType))
			return TypeHelper.compareSizes(sourceType, targetType, context) != 1;

		return TypeHelper.isFloatingPointType(sourceType) && TypeHelper.isBuiltinType(targetType, BuiltinType.FLOAT64);
	}

	private static boolean isValidNarrowCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		if(isOneOf(sourceType, BuiltinType.INT, BuiltinType.UINT, BuiltinType.PINT, BuiltinType.UPINT)
		|| isOneOf(targetType, BuiltinType.INT, BuiltinType.UINT, BuiltinType.PINT, BuiltinType.UPINT))
			return false;

		if(TypeHelper.isIntegerType(sourceType) && TypeHelper.isIntegerType(targetType))
			return TypeHelper.compareSizes(sourceType, targetType, context) != -1;

		if(TypeHelper.isBuiltinType(sourceType, BuiltinType.FLOAT32) && TypeHelper.isBuiltinType(targetType, BuiltinType.FLOAT64))
			return false;

		return TypeHelper.isFloatingPointType(sourceType) && TypeHelper.isFloatingPointType(targetType);
	}

	public static boolean isValidPointerCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		boolean sourceIsPointer = TypeHelper.isPointerType(sourceType);
		boolean sourceIsPointerInteger = isOneOf(sourceType, BuiltinType.PINT, BuiltinType.UPINT);
		boolean targetIsPointer = TypeHelper.isPointerType(targetType);
		boolean targetIsPointerInteger = isOneOf(targetType, BuiltinType.PINT, BuiltinType.UPINT);

		if(!sourceIsPointer && !sourceIsPointerInteger || !targetIsPointer && !targetIsPointerInteger)
			return false;

		if(SemaType.same(TypeHelper.removeConst(sourceType), TypeHelper.removeConst(targetType)))
			return true;

		return sourceIsPointer != targetIsPointer;
	}

	public static boolean isValidCast(SemaType sourceType, SemaType targetType, SemaExprCast.Kind kind, PlatformContext context)
	{
		switch(kind)
		{
		case SIGN_CAST:    return isValidSignCast(sourceType, targetType, context);
		case WIDEN_CAST:   return isValidWidenCast(sourceType, targetType, context);
		case NARROW_CAST:  return isValidNarrowCast(sourceType, targetType, context);
		case POINTER_CAST: return isValidPointerCast(sourceType, targetType, context);
		default: break;
		}

		throw new AssertionError("unreachable");
	}
}
