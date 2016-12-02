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

package katana.backend;

import katana.BuiltinFunc;
import katana.BuiltinType;
import katana.diag.CompileException;
import katana.platform.TargetTriple;
import katana.sema.decl.SemaDeclData;
import katana.utils.Maybe;

import java.math.BigInteger;
import java.util.List;

public abstract class PlatformContext
{
	private static final BigInteger TWO = BigInteger.valueOf(2);
	private static final BigInteger FOUR = BigInteger.valueOf(4);
	private static final BigInteger EIGHT = BigInteger.valueOf(8);

	private final TargetTriple triple;

	protected PlatformContext(TargetTriple triple)
	{
		this.triple = triple;
	}

	public abstract Maybe<BuiltinFunc> findBuiltin(String name);

	public BigInteger sizeof(BuiltinType builtin)
	{
		switch(builtin)
		{
		case VOID:
			throw new CompileException("sizeof applied to void type");

		case BOOL:
		case INT8:
		case UINT8:
			return BigInteger.ONE;

		case INT16:
		case UINT16:
			return TWO;

		case INT32:
		case UINT32:
		case FLOAT32:
			return FOUR;

		case INT64:
		case UINT64:
		case FLOAT64:
			return EIGHT;

		case INT:
		case UINT:
			return BigInteger.valueOf(triple.arch.intSize);

		case PINT:
		case UPINT:
		case NULL:
			return BigInteger.valueOf(triple.arch.pointerSize);

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	public BigInteger alignof(BuiltinType builtin)
	{
		return sizeof(builtin);
	}

	public BigInteger sizeof(SemaDeclData data)
	{
		throw new AssertionError("nyi");
	}

	public BigInteger alignof(SemaDeclData data)
	{
		List<SemaDeclData.Field> fields = data.fieldsByIndex();

		if(fields.isEmpty())
			return BigInteger.ONE;

		BigInteger max = fields.get(0).type.alignof(this);

		for(int i = 1; i != fields.size(); ++i)
		{
			BigInteger align = fields.get(i).type.alignof(this);

			if(align.compareTo(max) == 1)
				max = align;
		}

		return max;
	}
}
