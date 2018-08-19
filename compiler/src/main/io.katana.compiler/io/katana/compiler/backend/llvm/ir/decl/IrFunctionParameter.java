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

public class IrFunctionParameter
{
	public final IrType type;
	public final String name;
	public final boolean nonnull;

	public IrFunctionParameter(IrType type, String name, boolean nonnull)
	{
		this.type = type;
		this.name = name;
		this.nonnull = nonnull;
	}

	@Override
	public String toString()
	{
		var attributes = new StringBuilder();

		if(nonnull)
			attributes.append(" nonnull");

		return String.format("%s%s %%%s", type, attributes, name);
	}
}
