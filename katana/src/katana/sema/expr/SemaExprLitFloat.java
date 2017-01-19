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

import katana.BuiltinType;
import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeBuiltin;
import katana.sema.type.SemaTypeConst;

import java.math.BigDecimal;

public class SemaExprLitFloat extends SemaExprLiteral
{
	public SemaExprLitFloat(BigDecimal value, BuiltinType type)
	{
		this.value = value;
		this.type = type;

		SemaType semaType;

		switch(type)
		{
		case FLOAT32: semaType = SemaTypeBuiltin.FLOAT32; break;
		case FLOAT64: semaType = SemaTypeBuiltin.FLOAT64; break;

		default: throw new AssertionError("unreachable");
		}

		cachedType = new SemaTypeConst(semaType);
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}

	public final BigDecimal value;
	public final BuiltinType type;
	private final transient SemaType cachedType;
}
