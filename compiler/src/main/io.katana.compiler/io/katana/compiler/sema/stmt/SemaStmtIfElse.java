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

package io.katana.compiler.sema.stmt;

import io.katana.compiler.sema.expr.SemaExpr;

public class SemaStmtIfElse extends SemaStmt
{
	public boolean negated;
	public SemaExpr conditionExpr;
	public SemaStmt thenStmt;
	public SemaStmt elseStmt;

	public SemaStmtIfElse(boolean negated, SemaExpr conditionExpr, SemaStmt thenStmt, SemaStmt elseStmt)
	{
		this.negated = negated;
		this.conditionExpr = conditionExpr;
		this.thenStmt = thenStmt;
		this.elseStmt = elseStmt;
	}
}