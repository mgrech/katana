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

public class IrInstrBinary extends IrInstr
{
	public final IrValueSsa result;
	public final String name;
	public final IrType type;
	public final IrValue left;
	public final IrValue right;

	public IrInstrBinary(IrValueSsa result, String name, IrType type, IrValue left, IrValue right)
	{
		this.result = result;
		this.name = name;
		this.type = type;
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString()
	{
		return String.format("%s = %s %s %s, %s", result, name, type, left, right);
	}
}