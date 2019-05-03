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

import io.katana.compiler.backend.llvm.ir.type.IrType;
import io.katana.compiler.backend.llvm.ir.value.IrValueSsa;

public class IrInstrAlloca extends IrInstr
{
	public final IrValueSsa result;
	public final IrType type;
	public final long align;

	public IrInstrAlloca(IrValueSsa result, IrType type, long align)
	{
		this.result = result;
		this.type = type;
		this.align = align;
	}

	@Override
	public String toString()
	{
		return String.format("%s = alloca %s, align %s", result, type, align);
	}
}
