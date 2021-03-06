// Copyright 2016-2019 Markus Grech
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

package io.katana.compiler.ast.decl;

import io.katana.compiler.ExportKind;
import io.katana.compiler.ast.stmt.AstStmt;
import io.katana.compiler.ast.type.AstType;
import io.katana.compiler.utils.Maybe;

import java.util.List;

public class AstDeclFunctionDef extends AstDeclFunction
{
	public List<AstStmt> body;

	public AstDeclFunctionDef(ExportKind exportKind, String name, ParamList params, Maybe<AstType> returnType, List<AstStmt> body)
	{
		super(exportKind, name, params, returnType);
		this.body = body;
	}
}
