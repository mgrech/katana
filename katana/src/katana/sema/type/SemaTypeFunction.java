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

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

public class SemaTypeFunction extends SemaType
{
	public SemaTypeFunction(SemaType ret, List<SemaType> params)
	{
		this.ret = ret;
		this.params = params;
	}

	@Override
	public BigInteger sizeof(PlatformContext context)
	{
		throw new UnsupportedOperationException("sizeof");
	}

	@Override
	public BigInteger alignof(PlatformContext context)
	{
		throw new UnsupportedOperationException("alignof");
	}

	@Override
	protected boolean same(SemaType other)
	{
		SemaTypeFunction o = (SemaTypeFunction)other;

		if(!SemaType.same(ret, o.ret))
			return false;

		if(params.size() != o.params.size())
			return false;

		for(Iterator<SemaType> it1 = params.iterator(), it2 = o.params.iterator(); it1.hasNext();)
		{
			SemaType t1 = it1.next();
			SemaType t2 = it2.next();

			if(!SemaType.same(t1, t2))
				return false;
		}

		return true;
	}

	public SemaType ret;
	public List<SemaType> params;
}
