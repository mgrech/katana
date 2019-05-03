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

public class IrInstrStore extends IrInstr
{
	public final IrType type;
	public final IrValue value;
	public final IrValue pointer;

	public IrInstrStore(IrType type, IrValue value, IrValue pointer)
	{
		this.type = type;
		this.value = value;
		this.pointer = pointer;
	}

	@Override
	public String toString()
	{
		return String.format("store %s %s, %s* %s", type, value, type, pointer);
	}
}
