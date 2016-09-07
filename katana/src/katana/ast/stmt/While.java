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

package katana.ast.stmt;

import katana.ast.Expr;
import katana.ast.Stmt;

public class While extends Stmt
{
	public While(boolean negated, Expr condition, Stmt body)
	{
		this.negated = negated;
		this.condition = condition;
		this.body = body;
	}

	public boolean negated;
	public Expr condition;
	public Stmt body;
}
