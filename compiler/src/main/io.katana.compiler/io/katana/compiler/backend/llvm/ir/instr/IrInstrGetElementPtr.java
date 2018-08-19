// Copyright 2018 Markus Grech
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
import io.katana.compiler.backend.llvm.ir.value.IrValue;
import io.katana.compiler.backend.llvm.ir.value.IrValueSsa;

public class IrInstrGetElementPtr extends IrInstr
{
	public final IrValueSsa result;
	public final IrType baseType;
	public final IrValue compound;
	public final IrType indexType;
	public final IrValue index;

	public IrInstrGetElementPtr(IrValueSsa result, IrType baseType, IrValue compound, IrType indexType, IrValue index)
	{
		this.result = result;
		this.baseType = baseType;
		this.compound = compound;
		this.indexType = indexType;
		this.index = index;
	}

	@Override
	public String toString()
	{
		var fmt = "%s = getelementptr %s, %s* %s, i64 0, %s %s";
		return String.format(fmt, result, baseType, baseType, compound, indexType, index);
	}
}
