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

package io.katana.compiler.backend.llvm.ir.decl;

import io.katana.compiler.backend.llvm.ir.type.IrType;
import io.katana.compiler.backend.llvm.ir.value.IrValue;

public class IrDeclGlobalDef extends IrDecl
{
	public final String name;
	public final AddressMergeability mergeability;
	public final boolean constant;
	public final IrType type;
	public final IrValue initializer;

	public IrDeclGlobalDef(String name, AddressMergeability mergeability, boolean constant, IrType type, IrValue initializer)
	{
		this.name = name;
		this.mergeability = mergeability;
		this.constant = constant;
		this.type = type;
		this.initializer = initializer;
	}

	@Override
	public String toString()
	{
		var attributes = new StringBuilder();

		if(mergeability != AddressMergeability.NONE)
		{
			attributes.append(mergeability.toString().toLowerCase());
			attributes.append(' ');
		}

		attributes.append(constant ? "constant" : "global");
		return String.format("@%s = private %s %s %s\n", name, attributes, type, initializer);
	}
}
