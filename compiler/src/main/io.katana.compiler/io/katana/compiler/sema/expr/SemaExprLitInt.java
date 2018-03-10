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

import io.katana.compiler.BuiltinType;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.sema.type.SemaTypeConst;

import java.math.BigInteger;

public class SemaExprLitInt extends SimpleRValueExpr
{
	public final BigInteger value;
	public final BuiltinType type;
	private final transient SemaType cachedType;

	public SemaExprLitInt(BigInteger value, BuiltinType type)
	{
		this.value = value;
		this.type = type;

		SemaType semaType;

		switch(type)
		{
		case INT:   semaType = SemaTypeBuiltin.INT;   break;
		case INT8:  semaType = SemaTypeBuiltin.INT8;  break;
		case INT16: semaType = SemaTypeBuiltin.INT16; break;
		case INT32: semaType = SemaTypeBuiltin.INT32; break;
		case INT64: semaType = SemaTypeBuiltin.INT64; break;

		case UINT:   semaType = SemaTypeBuiltin.UINT;   break;
		case UINT8:  semaType = SemaTypeBuiltin.UINT8;  break;
		case UINT16: semaType = SemaTypeBuiltin.UINT16; break;
		case UINT32: semaType = SemaTypeBuiltin.UINT32; break;
		case UINT64: semaType = SemaTypeBuiltin.UINT64; break;

		default: throw new AssertionError("unreachable");
		}

		cachedType = new SemaTypeConst(semaType);
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}

	@Override
	public boolean isLiteral()
	{
		return true;
	}
}
