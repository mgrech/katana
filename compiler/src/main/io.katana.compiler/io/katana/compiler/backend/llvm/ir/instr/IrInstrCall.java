// Copyright 2018-2019 Markus Grech
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

package io.katana.compiler.backend.llvm.ir.instr;

import io.katana.compiler.Inlining;
import io.katana.compiler.backend.llvm.ir.type.IrType;
import io.katana.compiler.backend.llvm.ir.value.IrValue;
import io.katana.compiler.backend.llvm.ir.value.IrValueSsa;
import io.katana.compiler.utils.Maybe;

import java.util.List;

public class IrInstrCall extends IrInstr
{
	public final Maybe<IrValueSsa> result;
	public final IrType returnType;
	public final IrValue function;
	public final List<IrType> argTypes;
	public final List<IrValue> args;
	public final Inlining inline;

	public IrInstrCall(Maybe<IrValueSsa> result, IrType returnType, IrValue function, List<IrType> argTypes,
	                   List<IrValue> args, Inlining inline)
	{
		this.result = result;
		this.returnType = returnType;
		this.function = function;
		this.argTypes = argTypes;
		this.args = args;
		this.inline = inline;
	}

	@Override
	public String toString()
	{
		var builder = new StringBuilder();

		if(result.isSome())
			builder.append(String.format("%s = ", result.get()));

		builder.append(String.format("call %s %s(", returnType, function));

		if(!args.isEmpty())
		{
			builder.append(String.format("%s %s", argTypes.get(0), args.get(0)));

			for(var i = 1; i != args.size(); ++i)
				builder.append(String.format(", %s %s", argTypes.get(i), args.get(i)));
		}

		builder.append(')');

		switch(inline)
		{
		case AUTO:   break;
		case ALWAYS: builder.append(" alwaysinline");
		case NEVER:  builder.append(" noinline");
		default: throw new AssertionError("unreachable");
		}

		return builder.toString();
	}
}
