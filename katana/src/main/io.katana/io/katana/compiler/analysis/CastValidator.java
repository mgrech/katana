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

package io.katana.compiler.analysis;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.sema.expr.SemaExprCast;
import io.katana.compiler.sema.type.SemaType;

public class CastValidator
{
	private static boolean isValidSignCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		if(!Types.isInteger(sourceType) || !Types.isInteger(targetType))
			return false;

		return Types.equalSizes(sourceType, targetType, context);
	}

	private static boolean isOneOf(SemaType type, BuiltinType... builtinTypes)
	{
		for(BuiltinType builtinType : builtinTypes)
			if(Types.isBuiltin(type, builtinType))
				return true;

		return false;
	}

	private static boolean isValidWidenCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		if(isOneOf(sourceType, BuiltinType.INT, BuiltinType.UINT)
		|| isOneOf(targetType, BuiltinType.INT, BuiltinType.UINT))
			return false;

		if(Types.isInteger(sourceType) && Types.isInteger(targetType))
			return Types.compareSizes(sourceType, targetType, context) != 1;

		return Types.isFloatingPoint(sourceType) && Types.isBuiltin(targetType, BuiltinType.FLOAT64);
	}

	private static boolean isValidNarrowCast(SemaType sourceType, SemaType targetType, PlatformContext context)
	{
		if(isOneOf(sourceType, BuiltinType.INT, BuiltinType.UINT)
		|| isOneOf(targetType, BuiltinType.INT, BuiltinType.UINT))
			return false;

		if(Types.isInteger(sourceType) && Types.isInteger(targetType))
			return Types.compareSizes(sourceType, targetType, context) != -1;

		if(Types.isBuiltin(sourceType, BuiltinType.FLOAT32) && Types.isBuiltin(targetType, BuiltinType.FLOAT64))
			return false;

		return Types.isFloatingPoint(sourceType) && Types.isFloatingPoint(targetType);
	}

	private static boolean isValidPointerCast(SemaType sourceType, SemaType targetType)
	{
		boolean sourceIsPointer = Types.isPointer(sourceType);
		boolean sourceIsPointerInteger = isOneOf(sourceType, BuiltinType.INT, BuiltinType.UINT);
		boolean targetIsPointer = Types.isPointer(targetType);
		boolean targetIsPointerInteger = isOneOf(targetType, BuiltinType.INT, BuiltinType.UINT);

		if(!sourceIsPointer && !sourceIsPointerInteger || !targetIsPointer && !targetIsPointerInteger)
			return false;

		if(Types.equal(Types.removeConst(sourceType), Types.removeConst(targetType)))
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
		case POINTER_CAST: return isValidPointerCast(sourceType, targetType);
		default: break;
		}

		throw new AssertionError("unreachable");
	}
}
