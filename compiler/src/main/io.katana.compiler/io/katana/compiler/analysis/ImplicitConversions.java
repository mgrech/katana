// Copyright 2017 Markus Grech
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
import io.katana.compiler.sema.type.SemaTypeNullablePointer;

public class ImplicitConversions
{
	private static SemaExpr performPointerConversions(SemaExpr expr, SemaType targetType)
	{
		SemaType sourceType = expr.type();
		SemaType sourcePointeeType = Types.removePointer(sourceType);
		SemaType targetPointeeType = Types.removePointer(targetType);

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

		// !T -> !byte, ?T -> ?byte
		if(Types.isPointer(sourceType) && Types.isPointer(targetType) && Types.isByte(Types.removePointer(targetType)))
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
		SemaType sourceType = expr.type();
		SemaExpr rvalueExpr = ensureRValue(expr);

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

		return expr;
	}
}