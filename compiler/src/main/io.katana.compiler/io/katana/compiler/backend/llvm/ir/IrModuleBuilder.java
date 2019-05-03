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

package io.katana.compiler.backend.llvm.ir;

import io.katana.compiler.backend.llvm.ir.decl.*;
import io.katana.compiler.backend.llvm.ir.instr.IrInstr;
import io.katana.compiler.backend.llvm.ir.type.IrType;
import io.katana.compiler.backend.llvm.ir.value.IrValue;
import io.katana.compiler.platform.TargetTriple;

import java.util.ArrayList;
import java.util.List;

public class IrModuleBuilder
{
	private final List<IrDecl> decls = new ArrayList<>();

	public void append(IrDecl decl)
	{
		decls.add(decl);
	}

	public IrModule build()
	{
		return new IrModule(decls);
	}

	public void declareTargetTriple(TargetTriple triple)
	{
		decls.add(new IrDeclTargetTriple(triple));
	}

	public void defineType(String name, List<IrType> fields)
	{
		decls.add(new IrDeclTypeDef(name, fields));
	}

	public void defineGlobal(String name, AddressMergeability mergeability, boolean constant, IrType type, IrValue initializer)
	{
		decls.add(new IrDeclGlobalDef(name, mergeability, constant, type, initializer));
	}

	public void declareFunction(IrFunctionSignature signature)
	{
		decls.add(new IrDeclFunctionDecl(signature));
	}

	public void defineFunction(IrFunctionSignature signature, List<IrInstr> instructions)
	{
		decls.add(new IrDeclFunctionDef(signature, instructions));
	}
}
