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
import katana.sema.type.Builtin;
import katana.sema.type.Const;
import katana.sema.type.Type;
import katana.utils.Maybe;

import java.math.BigDecimal;

public class LitFloat extends Literal
{
	public LitFloat(BigDecimal value, BuiltinType type)
	{
		this.value = value;
		this.type = type;

		Type semaType;

		switch(type)
		{
		case FLOAT32: semaType = Builtin.FLOAT32; break;
		case FLOAT64: semaType = Builtin.FLOAT64; break;

		default: throw new AssertionError("unreachable");
		}

		cachedType = Maybe.some(new Const(semaType));
	}

	@Override
	public Maybe<Type> type()
	{
		return cachedType;
	}

	public final BigDecimal value;
	public final BuiltinType type;
	private final transient Maybe<Type> cachedType;
}
