// Copyright 2016 Markus Grech
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

package katana.sema.stmt;

import katana.sema.expr.SemaExpr;

public class SemaStmtWhile extends SemaStmt
{
	public SemaStmtWhile(boolean negated, SemaExpr condition, SemaStmt body)
	{
		this.negated = negated;
		this.condition = condition;
		this.body = body;
	}

	public boolean negated;
	public SemaExpr condition;
	public SemaStmt body;
}
