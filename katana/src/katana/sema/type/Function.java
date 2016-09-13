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

import katana.backend.PlatformContext;
import katana.utils.Maybe;

import java.util.Iterator;
import java.util.List;

public class Function extends Type
{
	public Function(Maybe<Type> ret, List<Type> params)
	{
		this.ret = ret;
		this.params = params;
	}

	@Override
	public int sizeof(PlatformContext context)
	{
		throw new UnsupportedOperationException("sizeof");
	}

	@Override
	public int alignof(PlatformContext context)
	{
		throw new UnsupportedOperationException("alignof");
	}

	@Override
	protected boolean same(Type other)
	{
		Function o = (Function)other;

		if(ret.isNone() != o.ret.isNone())
			return false;

		if(ret.isSome() && !Type.same(ret.unwrap(), o.ret.unwrap()))
			return false;

		if(params.size() != o.params.size())
			return false;

		for(Iterator<Type> it1 = params.iterator(), it2 = o.params.iterator(); it1.hasNext();)
		{
			Type t1 = it1.next();
			Type t2 = it2.next();

			if(!Type.same(t1, t2))
				return false;
		}

		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder paramString = new StringBuilder();

		if(!params.isEmpty())
		{
			paramString.append(params.get(0));

			for(int i = 1; i != params.size(); ++i)
			{
				paramString.append(", ");
				paramString.append(params.get(i));
			}
		}

		String retString = ret.isNone() ? "" : "=> " + ret.unwrap();

		return String.format("fn(%s)%s", paramString, retString);
	}

	public Maybe<Type> ret;
	public List<Type> params;
}
