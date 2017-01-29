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

package katana;

import katana.analysis.Types;
import katana.backend.PlatformContext;
import katana.sema.type.SemaTypeBuiltin;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

public class Limits
{
	private static final Map<BuiltinType, BigInteger> INT_MIN_VALUES = new TreeMap<>();
	private static final Map<BuiltinType, BigInteger> INT_MAX_VALUES = new TreeMap<>();
	private static final Map<BuiltinType, BigDecimal> FLOAT_MIN_VALUES = new TreeMap<>();
	private static final Map<BuiltinType, BigDecimal> FLOAT_MAX_VALUES = new TreeMap<>();

	static
	{
		INT_MIN_VALUES.put(BuiltinType.INT8,  BigInteger.valueOf(Byte.MIN_VALUE));
		INT_MIN_VALUES.put(BuiltinType.INT16, BigInteger.valueOf(Short.MIN_VALUE));
		INT_MIN_VALUES.put(BuiltinType.INT32, BigInteger.valueOf(Integer.MIN_VALUE));
		INT_MIN_VALUES.put(BuiltinType.INT64, BigInteger.valueOf(Long.MIN_VALUE));
		INT_MAX_VALUES.put(BuiltinType.INT8,  BigInteger.valueOf(Byte.MAX_VALUE));
		INT_MAX_VALUES.put(BuiltinType.INT16, BigInteger.valueOf(Short.MAX_VALUE));
		INT_MAX_VALUES.put(BuiltinType.INT32, BigInteger.valueOf(Integer.MAX_VALUE));
		INT_MAX_VALUES.put(BuiltinType.INT64, BigInteger.valueOf(Long.MAX_VALUE));

		INT_MIN_VALUES.put(BuiltinType.UINT8,  BigInteger.ZERO);
		INT_MIN_VALUES.put(BuiltinType.UINT16, BigInteger.ZERO);
		INT_MIN_VALUES.put(BuiltinType.UINT32, BigInteger.ZERO);
		INT_MIN_VALUES.put(BuiltinType.UINT64, BigInteger.ZERO);
		INT_MAX_VALUES.put(BuiltinType.UINT8,  BigInteger.valueOf((1L <<  8) - 1));
		INT_MAX_VALUES.put(BuiltinType.UINT16, BigInteger.valueOf((1L << 16) - 1));
		INT_MAX_VALUES.put(BuiltinType.UINT32, BigInteger.valueOf((1L << 32) - 1));

		BigInteger uint64Max = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
		INT_MAX_VALUES.put(BuiltinType.UINT64, uint64Max);

		FLOAT_MIN_VALUES.put(BuiltinType.FLOAT32, BigDecimal.valueOf(-Float.MAX_VALUE));
		FLOAT_MIN_VALUES.put(BuiltinType.FLOAT64, BigDecimal.valueOf(-Double.MAX_VALUE));
		FLOAT_MAX_VALUES.put(BuiltinType.FLOAT32, BigDecimal.valueOf(Float.MAX_VALUE));
		FLOAT_MAX_VALUES.put(BuiltinType.FLOAT64, BigDecimal.valueOf(Double.MAX_VALUE));
	}

	private static BuiltinType equivalentIntegerType(BuiltinType type, PlatformContext context)
	{
		if(type.kind != BuiltinType.Kind.INT && type.kind != BuiltinType.Kind.UINT)
			throw new AssertionError("unreachable");

		boolean isSigned = type.kind == BuiltinType.Kind.INT;

		switch(Types.sizeof(new SemaTypeBuiltin(type), context).intValue())
		{
		case 1: return isSigned ? BuiltinType.INT8  : BuiltinType.UINT8;
		case 2: return isSigned ? BuiltinType.INT16 : BuiltinType.UINT16;
		case 4: return isSigned ? BuiltinType.INT32 : BuiltinType.UINT32;
		case 8: return isSigned ? BuiltinType.INT64 : BuiltinType.UINT64;
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	public static BigInteger intMinValue(BuiltinType type, PlatformContext context)
	{
		if(type.kind != BuiltinType.Kind.INT && type.kind != BuiltinType.Kind.UINT)
			throw new AssertionError("unreachable");

		BigInteger min = INT_MIN_VALUES.get(type);

		if(min != null)
			return min;

		return INT_MIN_VALUES.get(equivalentIntegerType(type, context));
	}

	public static BigInteger intMaxValue(BuiltinType type, PlatformContext context)
	{
		if(type.kind != BuiltinType.Kind.INT && type.kind != BuiltinType.Kind.UINT)
			throw new AssertionError("unreachable");

		BigInteger max = INT_MAX_VALUES.get(type);

		if(max != null)
			return max;

		return INT_MAX_VALUES.get(equivalentIntegerType(type, context));
	}

	public static boolean inRange(BigInteger i, BuiltinType type, PlatformContext context)
	{
		BigInteger min = intMinValue(type, context);
		BigInteger max = intMaxValue(type, context);
		return i.compareTo(min) != -1 && i.compareTo(max) != 1;
	}

	public static boolean inRange(BigDecimal d, BuiltinType type)
	{
		BigDecimal min = FLOAT_MIN_VALUES.get(type);
		BigDecimal max = FLOAT_MAX_VALUES.get(type);
		return d.compareTo(min) != -1 && d.compareTo(max) != 1;
	}
}
