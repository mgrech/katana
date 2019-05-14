// Copyright 2017-2019 Markus Grech
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
			sourceType = Types.addNullablePointer(sourcePointeeType);
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
			var sourceElementType = Types.removeArray(sourcePointeeType);

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

	private static SemaExpr performSliceConversions(SemaExpr expr, SemaType targetType)
	{
		var sourceType = expr.type();
		var sourceElementType = Types.removeSlice(sourceType);
		var targetElementType = Types.removeSlice(targetType);

		// []T -> []const T
		if(!Types.isConst(sourceElementType) && Types.isConst(targetElementType))
			expr = new SemaExprImplicitConversionSliceToSliceOfConst(expr);

		// []T -> []byte, []const T -> []const byte
		if(!Types.isByte(sourceElementType) && Types.isByte(targetElementType))
			expr = new SemaExprImplicitConversionSliceToByteSlice(expr, targetType);

		return expr;
	}

	private static SemaExpr ensureRValue(SemaExpr expr)
	{
		return switch(expr.kind())
		{
		case RVALUE -> expr;
		case LVALUE -> new SemaExprImplicitConversionLValueToRValue(expr);
		};
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
		if(Types.isBuiltin(sourceType, BuiltinType.NULL) && Types.isNullablePointer(targetType))
			return new SemaExprImplicitConversionNullToNullablePointer(targetType);

		// null -> []T
		if(Types.isBuiltin(sourceType, BuiltinType.NULL) && Types.isSlice(targetType))
			return new SemaExprImplicitConversionNullToSlice(targetType);

		// pointer conversions
		if(Types.isPointer(sourceType) && Types.isPointer(targetType))
			return performPointerConversions(rvalueExpr, targetType);

		// slice conversions
		if(Types.isSlice(sourceType) && Types.isSlice(targetType))
			return performSliceConversions(rvalueExpr, targetType);

		// ![N]T -> []      T, ?[N]T -> []      T
		// ![N]T -> []const T, ?[N]T -> []const T
		// ![N]T -> []      byte, ?[N]T -> []      byte
		// ![N]T -> []const byte, ?[N]T -> []const byte
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

				if(Types.isByte(targetElementTypeNoConst))
					return new SemaExprImplicitConversionArrayPointerToByteSlice(rvalueExpr, targetType);
			}
		}

		return expr;
	}
}
