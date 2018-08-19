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

public class IrInstrInsertValue extends IrInstr
{
	public final IrValueSsa result;
	public final IrType compoundType;
	public final IrValue compound;
	public final IrType elementType;
	public final IrValue element;
	public final int index;

	public IrInstrInsertValue(IrValueSsa result, IrType compoundType, IrValue compound, IrType elementType, IrValue element, int index)
	{
		this.result = result;
		this.compoundType = compoundType;
		this.compound = compound;
		this.elementType = elementType;
		this.element = element;
		this.index = index;
	}

	@Override
	public String toString()
	{
		var fmt = "%s = insertvalue %s %s, %s %s, %s";
		return String.format(fmt, result, compoundType, compound, elementType, element, index);
	}
}
