// Copyright 2016-2019 Markus Grech
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

package io.katana.compiler.sema.type;

import io.katana.compiler.BuiltinType;

import java.util.EnumMap;

public class SemaTypeBuiltin extends SemaType
{
	private static final EnumMap<BuiltinType, SemaTypeBuiltin> INSTANCES = new EnumMap<>(BuiltinType.class);

	static
	{
		for(var type : BuiltinType.values())
			INSTANCES.put(type, new SemaTypeBuiltin(type));
	}

	public static final SemaTypeBuiltin VOID    = of(BuiltinType.VOID);
	public static final SemaTypeBuiltin BYTE    = of(BuiltinType.BYTE);
	public static final SemaTypeBuiltin BOOL    = of(BuiltinType.BOOL);
	public static final SemaTypeBuiltin INT8    = of(BuiltinType.INT8);
	public static final SemaTypeBuiltin INT16   = of(BuiltinType.INT16);
	public static final SemaTypeBuiltin INT32   = of(BuiltinType.INT32);
	public static final SemaTypeBuiltin INT64   = of(BuiltinType.INT64);
	public static final SemaTypeBuiltin INT     = of(BuiltinType.INT);
	public static final SemaTypeBuiltin UINT8   = of(BuiltinType.UINT8);
	public static final SemaTypeBuiltin UINT16  = of(BuiltinType.UINT16);
	public static final SemaTypeBuiltin UINT32  = of(BuiltinType.UINT32);
	public static final SemaTypeBuiltin UINT64  = of(BuiltinType.UINT64);
	public static final SemaTypeBuiltin UINT    = of(BuiltinType.UINT);
	public static final SemaTypeBuiltin FLOAT32 = of(BuiltinType.FLOAT32);
	public static final SemaTypeBuiltin FLOAT64 = of(BuiltinType.FLOAT64);
	public static final SemaTypeBuiltin NULL    = of(BuiltinType.NULL);

	public final BuiltinType which;

	private SemaTypeBuiltin(BuiltinType which)
	{
		this.which = which;
	}

	public static SemaTypeBuiltin of(BuiltinType type)
	{
		return INSTANCES.get(type);
	}
}
