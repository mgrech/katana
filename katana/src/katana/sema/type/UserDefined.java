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
import katana.sema.decl.Data;

import java.math.BigInteger;

public class UserDefined extends Type
{
	public UserDefined(Data data)
	{
		this.data = data;
	}

	@Override
	public BigInteger sizeof(PlatformContext context)
	{
		return context.sizeof(data);
	}

	@Override
	public BigInteger alignof(PlatformContext context)
	{
		return context.alignof(data);
	}

	@Override
	protected boolean same(Type other)
	{
		return data == ((UserDefined)other).data;
	}

	@Override
	public String toString()
	{
		return data.qualifiedName().toString();
	}

	public Data data;
}
