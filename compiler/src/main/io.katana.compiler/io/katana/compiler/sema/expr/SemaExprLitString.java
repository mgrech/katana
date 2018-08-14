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

import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;

import java.nio.charset.StandardCharsets;

public class SemaExprLitString extends SimpleRValueExpr
{
	private static final SemaType ELEMENT_TYPE = Types.addConst(SemaTypeBuiltin.UINT8);

	public final String value;
	private final transient SemaType cachedType;

	public SemaExprLitString(String value)
	{
		this.value = value;

		var length = value.getBytes(StandardCharsets.UTF_8).length;
		cachedType = Types.addNonNullablePointer(Types.addArray(length, ELEMENT_TYPE));
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
