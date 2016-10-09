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
import katana.sema.type.Array;
import katana.sema.type.Type;
import katana.utils.Maybe;

import java.math.BigInteger;
import java.util.List;

public class LitArray extends Literal
{
	public LitArray(BigInteger length, Type type, List<Expr> values)
	{
		this.length = length;
		this.type = type;
		this.values = values;
	}

	@Override
	public Maybe<Type> type()
	{
		return Maybe.some(new Array(length, TypeHelper.addConst(type)));
	}

	public final BigInteger length;
	public final Type type;
	public final List<Expr> values;
}
