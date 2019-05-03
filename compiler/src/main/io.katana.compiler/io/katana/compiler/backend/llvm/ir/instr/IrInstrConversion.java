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

public class IrInstrConversion extends IrInstr
{
	public enum Kind
	{
		ZEXT,
		SEXT,
		FPEXT,

		TRUNC,
		FPTRUNC,

		FPTOUI,
		FPTOSI,
		UITOFP,
		SITOFP,

		PTRTOINT,
		INTTOPTR,

		BITCAST,
	}

	public final IrValueSsa result;
	public final Kind kind;
	public final IrType sourceType;
	public final IrValue value;
	public final IrType targetType;

	public IrInstrConversion(IrValueSsa result, Kind kind, IrType sourceType, IrValue value, IrType targetType)
	{
		this.result = result;
		this.kind = kind;
		this.sourceType = sourceType;
		this.value = value;
		this.targetType = targetType;
	}

	@Override
	public String toString()
	{
		return String.format("%s = %s %s %s to %s", result, kind.toString().toLowerCase(), sourceType, value, targetType);
	}
}
