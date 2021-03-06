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

import io.katana.compiler.backend.llvm.ir.type.IrType;

import java.util.List;
import java.util.stream.Collectors;

public class IrDeclTypeDef extends IrDecl
{
	public final String name;
	public final List<IrType> fields;

	public IrDeclTypeDef(String name, List<IrType> fields)
	{
		this.name = name;
		this.fields = fields;
	}

	@Override
	public String toString()
	{
		var content = fields.stream()
		                    .map(IrType::toString)
		                    .collect(Collectors.joining(", "));

		return String.format("%%%s = type {%s}\n", name, content);
	}
}
