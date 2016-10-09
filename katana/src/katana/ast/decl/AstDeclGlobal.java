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

package katana.ast.decl;

import katana.ast.expr.AstExprLiteral;
import katana.ast.type.AstType;
import katana.utils.Maybe;

public class AstDeclGlobal extends AstDecl
{
	public AstDeclGlobal(boolean exported, boolean opaque, Maybe<AstType> type, String name, AstExprLiteral init)
	{
		super(exported, opaque);
		this.type = type;
		this.name = name;
		this.init = init;
	}

	public Maybe<AstType> type;
	public String name;
	public AstExprLiteral init;
}
