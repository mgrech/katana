// Copyright 2016-2019 Markus Grech
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

import java.util.List;

public class SemaExprLitArray extends SimpleRValueExpr
{
	public final SemaType elementType;
	public final List<SemaExpr> elementExprs;

	public SemaExprLitArray(SemaType elementType, List<SemaExpr> elementExprs)
	{
		this.elementType = elementType;
		this.elementExprs = elementExprs;
	}

	@Override
	public SemaType type()
	{
		return Types.addArray(elementExprs.size(), elementType);
	}
}
