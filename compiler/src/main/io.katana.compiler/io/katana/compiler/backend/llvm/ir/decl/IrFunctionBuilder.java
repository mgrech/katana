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

package io.katana.compiler.backend.llvm.ir.decl;

import io.katana.compiler.Inlining;
import io.katana.compiler.backend.llvm.ir.IrLabel;
import io.katana.compiler.backend.llvm.ir.instr.*;
import io.katana.compiler.backend.llvm.ir.type.IrType;
import io.katana.compiler.backend.llvm.ir.type.IrTypeFunction;
import io.katana.compiler.backend.llvm.ir.type.IrTypes;
import io.katana.compiler.backend.llvm.ir.value.IrValue;
import io.katana.compiler.backend.llvm.ir.value.IrValueSsa;
import io.katana.compiler.backend.llvm.ir.value.IrValues;
import io.katana.compiler.utils.Maybe;

import java.util.ArrayList;
import java.util.List;

public class IrFunctionBuilder
{
	private final List<IrInstr> instrs = new ArrayList<>();
	private int ssaCounter = 0;
	private int labelCounter = 0;

	private IrValueSsa allocateSsa()
	{
		return IrValues.ofSsa("$" + ssaCounter++);
	}

	public IrLabel allocateLabel(String name)
	{
		return IrLabel.of(String.format("%s$%s", name, labelCounter++));
	}

	public List<IrInstr> build()
	{
		return instrs;
	}

	public void alloca(IrValueSsa result, IrType type, long alignment)
	{
		instrs.add(new IrInstrAlloca(result, type, alignment));
	}

	public IrValueSsa alloca(IrType type, long alignment)
	{
		var result = allocateSsa();
		alloca(result, type, alignment);
		return result;
	}

	public IrValueSsa load(IrType type, IrValue pointer)
	{
		var result = allocateSsa();
		instrs.add(new IrInstrLoad(result, type, pointer));
		return result;
	}

	public void store(IrType type, IrValue value, IrValue pointer)
	{
		instrs.add(new IrInstrStore(type, value, pointer));
	}

	public IrValueSsa binary(String name, IrType type, IrValue left, IrValue right)
	{
		var result = allocateSsa();
		instrs.add(new IrInstrBinary(result, name, type, left, right));
		return result;
	}

	public IrValueSsa getelementptr(IrType baseType, IrValue compound, List<IrInstrGetElementPtr.Index> indices)
	{
		var result = allocateSsa();
		instrs.add(new IrInstrGetElementPtr(result, baseType, compound, indices));
		return result;
	}

	public IrValueSsa insertvalue(IrType compoundType, IrValue compound, IrType elementType, IrValue element, int index)
	{
		var result = allocateSsa();
		instrs.add(new IrInstrInsertValue(result, compoundType, compound, elementType, element, index));
		return result;
	}

	public IrValueSsa extractvalue(IrType compoundType, IrValue compound, int index)
	{
		var result = allocateSsa();
		instrs.add(new IrInstrExtractValue(result, compoundType, compound, index));
		return result;
	}

	public Maybe<IrValueSsa> call(IrTypeFunction type, IrValue function, List<IrType> argTypes, List<IrValue> args, Inlining inline)
	{
		var result = type.returnType == IrTypes.VOID
		             ? Maybe.<IrValueSsa>none()
		             : Maybe.some(allocateSsa());
		instrs.add(new IrInstrCall(result, type, function, argTypes, args, inline));
		return result;
	}

	public IrValueSsa convert(IrInstrConversion.Kind kind, IrType sourceType, IrValue value, IrType targetType)
	{
		var result = allocateSsa();
		instrs.add(new IrInstrConversion(result, kind, sourceType, value, targetType));
		return result;
	}

	public void label(String name)
	{
		instrs.add(new IrInstrLabel(name));
	}

	public void unreachable()
	{
		instrs.add(new IrInstrUnreachable());
	}

	public void ret(IrType type, Maybe<IrValue> value)
	{
		instrs.add(new IrInstrRet(type, value));
	}

	public void br(IrLabel label)
	{
		instrs.add(new IrInstrBrLabel(label));
	}

	public void br(IrValue condition, IrLabel trueLabel, IrLabel falseLabel)
	{
		instrs.add(new IrInstrBrCond(condition, trueLabel, falseLabel));
	}
}
