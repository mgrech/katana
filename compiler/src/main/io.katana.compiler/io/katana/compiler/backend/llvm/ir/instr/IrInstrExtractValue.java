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
import io.katana.compiler.backend.llvm.ir.value.IrValue;
import io.katana.compiler.backend.llvm.ir.value.IrValueSsa;

public class IrInstrExtractValue extends IrInstr
{
	public final IrValueSsa result;
	public final IrType compoundType;
	public final IrValue compound;
	public final int index;

	public IrInstrExtractValue(IrValueSsa result, IrType compoundType, IrValue compound, int index)
	{
		this.result = result;
		this.compoundType = compoundType;
		this.compound = compound;
		this.index = index;
	}

	@Override
	public String toString()
	{
		return String.format("%s = extractvalue %s %s, %s", result, compoundType, compound, index);
	}
}
