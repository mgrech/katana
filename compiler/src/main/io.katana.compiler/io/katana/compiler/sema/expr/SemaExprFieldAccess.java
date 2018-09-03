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

package io.katana.compiler.sema.expr;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.decl.SemaDeclStruct;
import io.katana.compiler.sema.type.SemaType;

public class SemaExprFieldAccess extends SemaExpr
{
	public SemaExpr structExpr;
	public SemaDeclStruct.Field field;
	public boolean resultIsConst;

	public SemaExprFieldAccess(SemaExpr structExpr, SemaDeclStruct.Field field, boolean resultIsConst)
	{
		this.structExpr = structExpr;
		this.field = field;
		this.resultIsConst = resultIsConst;
	}

	@Override
	public SemaType type()
	{
		return resultIsConst ? Types.addConst(field.type) : field.type;
	}

	@Override
	public ExprKind kind()
	{
		return structExpr.kind();
	}
}
