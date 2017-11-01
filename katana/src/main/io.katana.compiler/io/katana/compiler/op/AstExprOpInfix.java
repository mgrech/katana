// Copyright 2016-2017 Markus Grech
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

package io.katana.compiler.op;

import io.katana.compiler.ast.expr.AstExpr;
import io.katana.compiler.sema.decl.SemaDeclOperator;

public class AstExprOpInfix extends AstExpr
{
	public AstExpr left;
	public AstExpr right;
	public SemaDeclOperator decl;

	public AstExprOpInfix(AstExpr left, AstExpr right, SemaDeclOperator decl)
	{
		this.left = left;
		this.right = right;
		this.decl = decl;
	}
}
