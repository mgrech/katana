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

package katana.backend.llvm.amd64;

import katana.BuiltinType;
import katana.backend.llvm.PlatformContextLlvm;
import katana.sema.decl.Data;

import java.util.List;

public class PlatformContextLlvmAmd64 extends PlatformContextLlvm
{
	@Override
	public int sizeof(BuiltinType builtin)
	{
		switch(builtin)
		{
		case INT8:
		case UINT8:
			return 1;

		case INT16:
		case UINT16:
			return 2;

		case INT32:
		case UINT32:
			return 4;

		case INT64:
		case UINT64:
			return 8;

		case INT:
		case UINT:
		case PINT:
		case UPINT:
		case PTR:
			return 8;

		case BOOL:    return 1;
		case FLOAT32: return 4;
		case FLOAT64: return 8;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	@Override
	public int alignof(BuiltinType builtin)
	{
		return sizeof(builtin);
	}

	@Override
	public int sizeof(Data data)
	{
		return -1;
	}

	@Override
	public int alignof(Data data)
	{
		List<Data.Field> fields = data.fieldsByIndex();

		if(fields.isEmpty())
			return 1;

		int max = fields.get(0).type.alignof(this);

		for(int i = 1; i != fields.size(); ++i)
		{
			int align = fields.get(i).type.alignof(this);

			if(align > max)
				max = align;
		}

		return max;
	}
}
