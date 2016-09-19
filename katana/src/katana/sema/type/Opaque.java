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

public class Opaque extends Type
{
	public Opaque(BigInteger size, BigInteger alignment)
	{
		this.size = size;
		this.alignment = alignment;
	}

	@Override
	public BigInteger sizeof(PlatformContext context)
	{
		return size;
	}

	@Override
	public BigInteger alignof(PlatformContext context)
	{
		return alignment;
	}

	@Override
	protected boolean same(Type other)
	{
		Opaque o = (Opaque)other;
		return size.equals(o.size) && alignment.equals(o.alignment);
	}

	@Override
	public String toString()
	{
		return String.format("opaque(%s, %s)", size, alignment);
	}

	public BigInteger size;
	public BigInteger alignment;
}
