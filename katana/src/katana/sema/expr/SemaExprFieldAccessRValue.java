// Copyright 2016 Markus Grech
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

package katana.sema.expr;

import katana.analysis.TypeHelper;
import katana.sema.decl.SemaDeclStruct;
import katana.sema.type.SemaType;

public class SemaExprFieldAccessRValue extends SemaExpr
{
	public SemaExprFieldAccessRValue(SemaExpr expr, SemaDeclStruct.Field field, boolean const_)
	{
		this.expr = expr;
		this.field = field;
		this.const_ = const_;
	}

	@Override
	public SemaType type()
	{
		return const_ ? TypeHelper.addConst(field.type) : field.type;
	}

	public SemaExpr expr;
	public SemaDeclStruct.Field field;
	public boolean const_;
}
