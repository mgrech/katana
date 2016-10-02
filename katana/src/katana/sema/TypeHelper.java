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

package katana.sema;

import katana.sema.type.Array;
import katana.sema.type.Const;
import katana.sema.type.Function;
import katana.sema.type.Type;

public class TypeHelper
{
	public static boolean isConst(Type type)
	{
		if(type instanceof Array)
			return isConst(((Array)type).type);

		return type instanceof Const;
	}

	public static Type addConst(Type type)
	{
		if(type instanceof Function)
			throw new AssertionError("const added to function type");

		if(isConst(type))
			return type;

		if(type instanceof Array)
		{
			Array array = (Array)type;
			return new Array(array.length, addConst(array.type));
		}

		return new Const(type);
	}

	public static Type removeConst(Type type)
	{
		if(type instanceof Function)
			throw new AssertionError("const removed from function type");

		if(type instanceof Const)
			return ((Const)type).type;

		if(type instanceof Array)
		{
			Array array = (Array)type;
			return new Array(array.length, removeConst(array.type));
		}

		return type;
	}
}
