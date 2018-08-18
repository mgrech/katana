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

package io.katana.compiler.backend.llvm.ir;

import java.util.List;
import java.util.stream.Collectors;

public class IrDeclFunctionDef extends IrDecl
{
	public final IrFunctionSignature signature;
	public final List<IrInstr> instructions;

	public IrDeclFunctionDef(IrFunctionSignature signature, List<IrInstr> instructions)
	{
		this.signature = signature;
		this.instructions = instructions;
	}

	private String toString(IrInstr instr)
	{
		var builder = new StringBuilder();

		if(!(instr instanceof IrInstrLabel))
			builder.append('\t');

		builder.append(instr);
		builder.append('\n');
		return builder.toString();
	}

	@Override
	public String toString()
	{
		var instrs = instructions.stream()
		                         .map(this::toString)
		                         .collect(Collectors.joining());

		return String.format("define %s\n{\n%s}\n", signature, instrs);
	}
}
