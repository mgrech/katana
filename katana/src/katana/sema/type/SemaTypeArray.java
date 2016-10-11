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

public class SemaTypeArray extends SemaType
{
	public SemaTypeArray(BigInteger length, SemaType type)
	{
		this.length = length;
		this.type = type;
	}

	@Override
	public BigInteger sizeof(PlatformContext context)
	{
		return length.multiply(type.sizeof(context));
	}

	@Override
	public BigInteger alignof(PlatformContext context)
	{
		return type.alignof(context);
	}

	@Override
	protected boolean same(SemaType other)
	{
		SemaTypeArray o = (SemaTypeArray)other;
		return length.equals(o.length) && SemaType.same(type, o.type);
	}

	@Override
	public String toString()
	{
		return String.format("[%s]%s", length, type);
	}

	public BigInteger length;
	public SemaType type;
}