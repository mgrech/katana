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

package katana.sema.type;

import katana.BuiltinType;

public class SemaTypeBuiltin extends SemaType
{
	public static final SemaTypeBuiltin VOID    = new SemaTypeBuiltin(BuiltinType.VOID);
	public static final SemaTypeBuiltin BOOL    = new SemaTypeBuiltin(BuiltinType.BOOL);
	public static final SemaTypeBuiltin INT8    = new SemaTypeBuiltin(BuiltinType.INT8);
	public static final SemaTypeBuiltin INT16   = new SemaTypeBuiltin(BuiltinType.INT16);
	public static final SemaTypeBuiltin INT32   = new SemaTypeBuiltin(BuiltinType.INT32);
	public static final SemaTypeBuiltin INT64   = new SemaTypeBuiltin(BuiltinType.INT64);
	public static final SemaTypeBuiltin INT     = new SemaTypeBuiltin(BuiltinType.INT);
	public static final SemaTypeBuiltin PINT    = new SemaTypeBuiltin(BuiltinType.PINT);
	public static final SemaTypeBuiltin UINT8   = new SemaTypeBuiltin(BuiltinType.UINT8);
	public static final SemaTypeBuiltin UINT16  = new SemaTypeBuiltin(BuiltinType.UINT16);
	public static final SemaTypeBuiltin UINT32  = new SemaTypeBuiltin(BuiltinType.UINT32);
	public static final SemaTypeBuiltin UINT64  = new SemaTypeBuiltin(BuiltinType.UINT64);
	public static final SemaTypeBuiltin UINT    = new SemaTypeBuiltin(BuiltinType.UINT);
	public static final SemaTypeBuiltin UPINT   = new SemaTypeBuiltin(BuiltinType.UPINT);
	public static final SemaTypeBuiltin FLOAT32 = new SemaTypeBuiltin(BuiltinType.FLOAT32);
	public static final SemaTypeBuiltin FLOAT64 = new SemaTypeBuiltin(BuiltinType.FLOAT64);
	public static final SemaTypeBuiltin NULL    = new SemaTypeBuiltin(BuiltinType.NULL);

	public SemaTypeBuiltin(BuiltinType which)
	{
		this.which = which;
	}

	@Override
	protected boolean same(SemaType other)
	{
		return which == ((SemaTypeBuiltin)other).which;
	}

	public BuiltinType which;
}
