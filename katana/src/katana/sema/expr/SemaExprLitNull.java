// Copyright 2016-2017 Markus Grech
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

package katana.sema.expr;

import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeBuiltin;
import katana.sema.type.SemaTypeConst;

public class SemaExprLitNull extends SimpleRValueExpr
{
	private static final SemaType TYPE = new SemaTypeConst(SemaTypeBuiltin.NULL);

	@Override
	public SemaType type()
	{
		return TYPE;
	}

	@Override
	public boolean isLiteral()
	{
		return true;
	}
}
