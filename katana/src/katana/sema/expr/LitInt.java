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
import katana.sema.type.Type;
import katana.utils.Maybe;

import java.math.BigInteger;

public class LitInt extends Expr
{
	public LitInt(BigInteger value, BuiltinType type)
	{
		this.value = value;
		this.type = type;

		switch(type)
		{
		case INT:   cachedType = Builtin.INT;   break;
		case PINT:  cachedType = Builtin.PINT;  break;
		case INT8:  cachedType = Builtin.INT8;  break;
		case INT16: cachedType = Builtin.INT16; break;
		case INT32: cachedType = Builtin.INT32; break;
		case INT64: cachedType = Builtin.INT64; break;

		case UINT:   cachedType = Builtin.UINT;   break;
		case UPINT:  cachedType = Builtin.UPINT;  break;
		case UINT8:  cachedType = Builtin.UINT8;  break;
		case UINT16: cachedType = Builtin.UINT16; break;
		case UINT32: cachedType = Builtin.UINT32; break;
		case UINT64: cachedType = Builtin.UINT64; break;

		default: throw new AssertionError("unreachable");
		}
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(cachedType);
	}

	public BigInteger value;
	public BuiltinType type;
	private Type cachedType;
}
