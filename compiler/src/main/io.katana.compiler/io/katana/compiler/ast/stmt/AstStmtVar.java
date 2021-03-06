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

package io.katana.compiler.ast.stmt;

import io.katana.compiler.ast.expr.AstExpr;
import io.katana.compiler.ast.type.AstType;
import io.katana.compiler.utils.Maybe;

public class AstStmtVar extends AstStmt
{
	public Maybe<AstType> type;
	public String name;
	public Maybe<AstExpr> initializerExpr;

	public AstStmtVar(Maybe<AstType> type, String name, Maybe<AstExpr> initializerExpr)
	{
		this.type = type;
		this.name = name;
		this.initializerExpr = initializerExpr;
	}
}
