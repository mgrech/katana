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

package katana.analysis;

import katana.BuiltinType;
import katana.sema.expr.*;
import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeNullablePointer;

public class ImplicitConversions
{
	private static SemaExpr performPointerConversions(SemaExpr expr, SemaType targetType)
	{
		SemaType sourceType = expr.type();
		SemaType sourcePointeeType = TypeHelper.removePointer(sourceType);
		SemaType targetPointeeType = TypeHelper.removePointer(targetType);

		// !T -> ?T
		if(TypeHelper.isNonNullablePointerType(sourceType) && TypeHelper.isNullablePointerType(targetType))
		{
			sourceType = new SemaTypeNullablePointer(sourcePointeeType);
			expr = new SemaExprImplicitConversionNonNullablePointerToNullablePointer(expr, sourceType);
		}

		// !T -> !const T, ?T -> ?const T
		if(!TypeHelper.isConst(sourcePointeeType) && TypeHelper.isConst(targetPointeeType))
		{
			sourcePointeeType = TypeHelper.addConst(sourcePointeeType);
			sourceType = TypeHelper.copyPointerKind(sourceType, sourcePointeeType);
			expr = new SemaExprImplicitConversionPointerToNonConstToPointerToConst(expr, sourceType);
		}

		// !T -> !void, ?T -> ?void
		if(TypeHelper.isAnyPointerType(sourceType) && TypeHelper.isAnyPointerType(targetType)
			&& TypeHelper.isVoidType(TypeHelper.removePointer(targetType)))
			expr = new SemaExprImplicitConversionPointerToVoidPointer(expr, targetType);

		return expr;
	}

	public static SemaExpr perform(SemaExpr expr, SemaType targetType)
	{
		SemaType sourceType = expr.type();

		// null -> ?T
		if(TypeHelper.isNullablePointerType(targetType) && TypeHelper.isBuiltinType(sourceType, BuiltinType.NULL))
			return new SemaExprImplicitConversionNullToNullablePointer(targetType);

		// !T, ?T
		if(TypeHelper.isAnyPointerType(sourceType) && TypeHelper.isAnyPointerType(targetType))
			return performPointerConversions(expr, targetType);

		return expr;
	}
}
