// Copyright 2018-2019 Markus Grech
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

package io.katana.compiler.sema.expr;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.type.SemaType;

public class SemaExprSliceGetPointer extends SemaExpr
{
	public final SemaExpr sliceExpr;
	private final transient SemaType cachedType;

	public SemaExprSliceGetPointer(SemaExpr sliceExpr)
	{
		this.sliceExpr = sliceExpr;
		this.cachedType = Types.addNonNullablePointer(Types.removeSlice(sliceExpr.type()));
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}

	@Override
	public ExprKind kind()
	{
		return sliceExpr.kind();
	}
}
