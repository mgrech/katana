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
import katana.sema.expr.SemaExpr;
import katana.sema.expr.SemaExprImplicitConversionNonNullablePointerToNullablePointer;
import katana.sema.expr.SemaExprImplicitConversionNullToNullablePointer;
import katana.sema.type.SemaType;

public class ImplicitConversions
{
	public static SemaExpr perform(SemaExpr expr, SemaType targetType)
	{
		SemaType sourceType = expr.type();

		if(TypeHelper.isNullablePointerType(targetType) && TypeHelper.isBuiltinType(sourceType, BuiltinType.NULL))
			return new SemaExprImplicitConversionNullToNullablePointer(targetType);

		if(TypeHelper.isNonNullablePointerType(sourceType) && TypeHelper.isNullablePointerType(targetType))
			return new SemaExprImplicitConversionNonNullablePointerToNullablePointer(expr, targetType);

		return expr;
	}
}
