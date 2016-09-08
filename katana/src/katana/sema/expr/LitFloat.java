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

import katana.BuiltinType;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.type.Builtin;
import katana.utils.Maybe;

import java.math.BigDecimal;

public class LitFloat extends Expr
{
	public LitFloat(BigDecimal value, BuiltinType type)
	{
		this.value = value;
		this.type = type;

		switch(type)
		{
		case FLOAT32: cachedType = Builtin.FLOAT32; break;
		case FLOAT64: cachedType = Builtin.FLOAT64; break;

		default: throw new AssertionError("unreachable");
		}
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(cachedType);
	}

	public BigDecimal value;
	public BuiltinType type;
	private Type cachedType;
}
