// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.sema.expr;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Fraction;

public class SemaExprLitFloat extends SimpleRValueExpr
{
	public final Fraction value;
	public final BuiltinType type;
	private final transient SemaType cachedType;

	public SemaExprLitFloat(Fraction value, BuiltinType type)
	{
		this.value = value;
		this.type = type;
		this.cachedType = Types.addConst(SemaTypeBuiltin.of(type));
	}

	@Override
	public SemaType type()
	{
		return cachedType;
	}

	@Override
	public boolean isLiteral()
	{
		return true;
	}
}
