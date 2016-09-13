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

package katana.ast.type;

import katana.BuiltinType;

public class Builtin extends Type
{

	public static final Builtin BOOL    = new Builtin(BuiltinType.BOOL);
	public static final Builtin INT8    = new Builtin(BuiltinType.INT8);
	public static final Builtin INT16   = new Builtin(BuiltinType.INT16);
	public static final Builtin INT32   = new Builtin(BuiltinType.INT32);
	public static final Builtin INT64   = new Builtin(BuiltinType.INT64);
	public static final Builtin INT     = new Builtin(BuiltinType.INT);
	public static final Builtin PINT    = new Builtin(BuiltinType.PINT);
	public static final Builtin UINT8   = new Builtin(BuiltinType.UINT8);
	public static final Builtin UINT16  = new Builtin(BuiltinType.UINT16);
	public static final Builtin UINT32  = new Builtin(BuiltinType.UINT32);
	public static final Builtin UINT64  = new Builtin(BuiltinType.UINT64);
	public static final Builtin UINT    = new Builtin(BuiltinType.UINT);
	public static final Builtin UPINT   = new Builtin(BuiltinType.UPINT);
	public static final Builtin FLOAT32 = new Builtin(BuiltinType.FLOAT32);
	public static final Builtin FLOAT64 = new Builtin(BuiltinType.FLOAT64);
	public static final Builtin PTR     = new Builtin(BuiltinType.PTR);

	private Builtin(BuiltinType which)
	{
		this.which = which;
	}

	public final BuiltinType which;

	@Override
	public String toString()
	{
		return which.toString().toLowerCase();
	}
}
