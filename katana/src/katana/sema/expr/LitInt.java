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

import java.math.BigInteger;

public class LitInt extends Literal
{
	public LitInt(BigInteger value, BuiltinType type)
	{
		this.value = value;
		this.type = type;

		Type semaType;

		switch(type)
		{
		case INT:   semaType = Builtin.INT;   break;
		case PINT:  semaType = Builtin.PINT;  break;
		case INT8:  semaType = Builtin.INT8;  break;
		case INT16: semaType = Builtin.INT16; break;
		case INT32: semaType = Builtin.INT32; break;
		case INT64: semaType = Builtin.INT64; break;

		case UINT:   semaType = Builtin.UINT;   break;
		case UPINT:  semaType = Builtin.UPINT;  break;
		case UINT8:  semaType = Builtin.UINT8;  break;
		case UINT16: semaType = Builtin.UINT16; break;
		case UINT32: semaType = Builtin.UINT32; break;
		case UINT64: semaType = Builtin.UINT64; break;

		default: throw new AssertionError("unreachable");
		}

		cachedType = Maybe.some(new Const(semaType));
	}

	@Override
	public Maybe<Type> type()
	{
		return cachedType;
	}

	public final BigInteger value;
	public final BuiltinType type;
	private final transient Maybe<Type> cachedType;
}
