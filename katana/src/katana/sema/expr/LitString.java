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

import katana.sema.type.Array;
import katana.sema.type.Builtin;
import katana.sema.type.Type;
import katana.utils.Maybe;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class LitString extends SimpleLValueExpr
{
	public LitString(String value)
	{
		this.value = value;
	}

	@Override
	public Maybe<Type> type()
	{
		BigInteger length = BigInteger.valueOf(value.getBytes(StandardCharsets.UTF_8).length);
		return Maybe.some(new Array(length, Builtin.UINT8));
	}

	public String value;
}
