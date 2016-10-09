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

package katana.sema.expr;

import katana.analysis.TypeHelper;
import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeArray;
import katana.utils.Maybe;

import java.math.BigInteger;
import java.util.List;

public class SemaExprLitArray extends SemaExprLiteral
{
	public SemaExprLitArray(BigInteger length, SemaType type, List<SemaExpr> values)
	{
		this.length = length;
		this.type = type;
		this.values = values;
	}

	@Override
	public Maybe<SemaType> type()
	{
		return Maybe.some(new SemaTypeArray(length, TypeHelper.addConst(type)));
	}

	public final BigInteger length;
	public final SemaType type;
	public final List<SemaExpr> values;
}
