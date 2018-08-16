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

package io.katana.compiler;

import io.katana.compiler.backend.llvm.FunctionCodegenContext;
import io.katana.compiler.backend.llvm.ir.IrValueSsa;
import io.katana.compiler.sema.expr.SemaExprBuiltinCall;
import io.katana.compiler.sema.type.SemaType;

import java.util.List;

public abstract class BuiltinFunc
{
	public final String name;

	protected BuiltinFunc(String name)
	{
		this.name = name;
	}

	public abstract SemaType validateCall(List<SemaType> args);
	public abstract IrValueSsa generateCall(SemaExprBuiltinCall call, FunctionCodegenContext context);
}
