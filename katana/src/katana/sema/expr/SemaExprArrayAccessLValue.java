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

package katana.sema.expr;

import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeArray;

public class SemaExprArrayAccessLValue extends SemaExprLValueExpr
{
	public SemaExprLValueExpr value;
	public SemaExpr index;

	public SemaExprArrayAccessLValue(SemaExprLValueExpr value, SemaExpr index)
	{
		this.value = value;
		this.index = index;
	}

	@Override
	public void useAsLValue(boolean use)
	{
		value.useAsLValue(use);
	}

	@Override
	public boolean isUsedAsLValue()
	{
		return value.isUsedAsLValue();
	}

	@Override
	public SemaType type()
	{
		return ((SemaTypeArray)value.type()).type;
	}
}
