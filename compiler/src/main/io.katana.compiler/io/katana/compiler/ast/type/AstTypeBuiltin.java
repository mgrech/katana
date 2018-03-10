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

package io.katana.compiler.ast.type;

import io.katana.compiler.BuiltinType;

public class AstTypeBuiltin extends AstType
{
	public static final AstTypeBuiltin VOID    = new AstTypeBuiltin(BuiltinType.VOID);
	public static final AstTypeBuiltin BYTE    = new AstTypeBuiltin(BuiltinType.BYTE);
	public static final AstTypeBuiltin BOOL    = new AstTypeBuiltin(BuiltinType.BOOL);
	public static final AstTypeBuiltin INT8    = new AstTypeBuiltin(BuiltinType.INT8);
	public static final AstTypeBuiltin INT16   = new AstTypeBuiltin(BuiltinType.INT16);
	public static final AstTypeBuiltin INT32   = new AstTypeBuiltin(BuiltinType.INT32);
	public static final AstTypeBuiltin INT64   = new AstTypeBuiltin(BuiltinType.INT64);
	public static final AstTypeBuiltin INT     = new AstTypeBuiltin(BuiltinType.INT);
	public static final AstTypeBuiltin UINT8   = new AstTypeBuiltin(BuiltinType.UINT8);
	public static final AstTypeBuiltin UINT16  = new AstTypeBuiltin(BuiltinType.UINT16);
	public static final AstTypeBuiltin UINT32  = new AstTypeBuiltin(BuiltinType.UINT32);
	public static final AstTypeBuiltin UINT64  = new AstTypeBuiltin(BuiltinType.UINT64);
	public static final AstTypeBuiltin UINT    = new AstTypeBuiltin(BuiltinType.UINT);
	public static final AstTypeBuiltin FLOAT32 = new AstTypeBuiltin(BuiltinType.FLOAT32);
	public static final AstTypeBuiltin FLOAT64 = new AstTypeBuiltin(BuiltinType.FLOAT64);

	public final BuiltinType which;

	private AstTypeBuiltin(BuiltinType which)
	{
		this.which = which;
	}
}
