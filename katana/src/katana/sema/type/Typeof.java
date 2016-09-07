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
import katana.sema.Expr;
import katana.sema.Type;

public class Typeof extends Type
{
	public Typeof(Expr expr)
	{
		this.expr = expr;
	}

	@Override
	public int sizeof(PlatformContext context)
	{
		return expr.type().unwrap().sizeof(context);
	}

	@Override
	public int alignof(PlatformContext context)
	{
		return expr.type().unwrap().alignof(context);
	}

	@Override
	protected boolean same(Type other)
	{
		Type first = expr.type().unwrap();
		Type second = ((Typeof)other).expr.type().unwrap();
		return Type.same(first, second);
	}

	public Expr expr;
}
