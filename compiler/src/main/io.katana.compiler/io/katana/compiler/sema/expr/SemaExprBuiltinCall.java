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

package io.katana.compiler.sema.expr;

import io.katana.compiler.BuiltinFunc;
import io.katana.compiler.sema.type.SemaType;

import java.util.List;

public class SemaExprBuiltinCall extends SimpleRValueExpr
{
	public BuiltinFunc func;
	public List<SemaExpr> args;
	public SemaType ret;

	public SemaExprBuiltinCall(BuiltinFunc func, List<SemaExpr> args, SemaType ret)
	{
		this.func = func;
		this.args = args;
		this.ret = ret;
	}

	@Override
	public SemaType type()
	{
		return ret;
	}
}