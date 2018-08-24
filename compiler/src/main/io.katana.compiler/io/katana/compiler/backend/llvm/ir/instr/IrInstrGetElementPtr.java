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

import java.util.List;
import java.util.stream.Collectors;

public class IrInstrGetElementPtr extends IrInstr
{
	public static class Index
	{
		public final IrType type;
		public final IrValue value;

		public Index(IrType type, IrValue value)
		{
			this.type = type;
			this.value = value;
		}

		@Override
		public String toString()
		{
			return String.format("%s %s", type, value);
		}
	}

	public final IrValueSsa result;
	public final IrType baseType;
	public final IrValue compound;
	public final List<Index> indices;

	public IrInstrGetElementPtr(IrValueSsa result, IrType baseType, IrValue compound, List<Index> indices)
	{
		this.result = result;
		this.baseType = baseType;
		this.compound = compound;
		this.indices = indices;
	}

	@Override
	public String toString()
	{
		var indicesString = indices.stream()
		                           .map(Index::toString)
		                           .collect(Collectors.joining(", "));

		var fmt = "%s = getelementptr %s, %s* %s, %s";
		return String.format(fmt, result, baseType, baseType, compound, indicesString);
	}
}
