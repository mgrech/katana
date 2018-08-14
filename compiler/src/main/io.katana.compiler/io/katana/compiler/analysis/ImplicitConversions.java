// Copyright 2017-2018 Markus Grech
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
import io.katana.compiler.sema.expr.*;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeArray;
import io.katana.compiler.sema.type.SemaTypeNullablePointer;

public class ImplicitConversions
{
	private static SemaExpr performPointerConversions(SemaExpr expr, SemaType targetType)
	{
		var sourceType = expr.type();
		var sourcePointeeType = Types.removePointer(sourceType);
		var targetPointeeType = Types.removePointer(targetType);

		// !T -> ?T
		if(Types.isNonNullablePointer(sourceType) && Types.isNullablePointer(targetType))
		{
			sourceType = new SemaTypeNullablePointer(sourcePointeeType);
			expr = new SemaExprImplicitConversionNonNullablePointerToNullablePointer(expr, sourceType);
		}

		// !T -> !const T, ?T -> ?const T
		if(!Types.isConst(sourcePointeeType) && Types.isConst(targetPointeeType))
		{
			sourcePointeeType = Types.addConst(sourcePointeeType);
			sourceType = Types.copyPointerKind(sourceType, sourcePointeeType);
			expr = new SemaExprImplicitConversionPointerToNonConstToPointerToConst(expr, sourceType);
		}

		// ![N]T -> !T, ?[N]T -> ?T
		if(Types.isArray(sourcePointeeType))
		{
			var sourceElementType = ((SemaTypeArray)sourcePointeeType).type;

			if(Types.equal(sourceElementType, targetPointeeType))
			{
				expr = new SemaExprImplicitConversionArrayPointerToPointer(expr);
				sourceType = expr.type();
				sourcePointeeType = Types.removePointer(sourceType);
			}
		}

		// !T -> !byte, ?T -> ?byte
		if(Types.isPointer(sourceType) && Types.isPointer(targetType)
		&& !Types.isByte(Types.removePointer(sourceType)) && Types.isByte(Types.removePointer(targetType)))
			expr = new SemaExprImplicitConversionPointerToBytePointer(expr, targetType);

		return expr;
	}

	private static SemaExpr ensureRValue(SemaExpr expr)
	{
		switch(expr.kind())
		{
		case RVALUE: return expr;
		case LVALUE: return new SemaExprImplicitConversionLValueToRValue(expr);
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	public static SemaExpr perform(SemaExpr expr, SemaType targetType)
	{
		var sourceType = expr.type();
		var rvalueExpr = ensureRValue(expr);

		// float32 -> float64
		if(Types.isBuiltin(sourceType, BuiltinType.FLOAT32) && Types.isBuiltin(targetType, BuiltinType.FLOAT64))
			return new SemaExprImplicitConversionWiden(rvalueExpr, targetType);

		// (u)intN -> (u)intM (widen)
		if(Types.isFixedSizeInteger(sourceType) && Types.isFixedSizeInteger(targetType)
		&& Types.isSigned(sourceType) == Types.isSigned(targetType)
		&& Types.compareSizes(sourceType, targetType, null) == -1)
			return new SemaExprImplicitConversionWiden(rvalueExpr, targetType);

		// null -> ?T
		if(Types.isNullablePointer(targetType) && Types.isBuiltin(sourceType, BuiltinType.NULL))
			return new SemaExprImplicitConversionNullToNullablePointer(targetType);

		// pointer conversions
		if(Types.isPointer(sourceType) && Types.isPointer(targetType))
			return performPointerConversions(rvalueExpr, targetType);

		// []T -> []const T
		if(Types.isSlice(sourceType) && !Types.isConst(Types.removeSlice(sourceType))
		&& Types.isSlice(targetType) && Types.isConst(Types.removeSlice(targetType)))
			return new SemaExprImplicitConversionSliceToSliceOfConst(rvalueExpr);

		// ![N]T -> []      T, ?[N]T -> []      T
		// ![N]T -> []const T, ?[N]T -> []const T
		if(Types.isPointer(sourceType) && Types.isSlice(targetType))
		{
			var sourcePointeeType = Types.removePointer(sourceType);

			if(Types.isArray(sourcePointeeType))
			{
				var sourceElementType = Types.removeArray(sourcePointeeType);
				var targetElementType = Types.removeSlice(targetType);
				var targetElementTypeNoConst = Types.removeConst(targetElementType);

				if(Types.equal(sourceElementType, targetElementTypeNoConst))
					return new SemaExprImplicitConversionArrayPointerToSlice(rvalueExpr, targetType);
			}
		}

		return expr;
	}
}
