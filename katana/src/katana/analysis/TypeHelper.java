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

package katana.analysis;

import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeArray;
import katana.sema.type.SemaTypeConst;
import katana.sema.type.SemaTypeFunction;

public class TypeHelper
{
	public static boolean isConst(SemaType type)
	{
		if(type instanceof SemaTypeArray)
			return isConst(((SemaTypeArray)type).type);

		return type instanceof SemaTypeConst;
	}

	public static SemaType addConst(SemaType type)
	{
		if(type instanceof SemaTypeFunction)
			throw new AssertionError("const added to function type");

		if(isConst(type))
			return type;

		if(type instanceof SemaTypeArray)
		{
			SemaTypeArray array = (SemaTypeArray)type;
			return new SemaTypeArray(array.length, addConst(array.type));
		}

		return new SemaTypeConst(type);
	}

	public static SemaType removeConst(SemaType type)
	{
		if(type instanceof SemaTypeConst)
			return ((SemaTypeConst)type).type;

		if(type instanceof SemaTypeArray)
		{
			SemaTypeArray array = (SemaTypeArray)type;
			return new SemaTypeArray(array.length, removeConst(array.type));
		}

		return type;
	}

	public static SemaType decay(SemaType type)
	{
		return removeConst(type);
	}

	public static boolean decayedEqual(SemaType a, SemaType b)
	{
		return SemaType.same(decay(a), decay(b));
	}
}