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

import katana.ast.stmt.AstStmt;
import katana.ast.type.AstType;
import katana.utils.Maybe;

import java.util.ArrayList;
import java.util.List;

public class AstDeclDefinedFunction extends AstDeclFunction
{
	public AstDeclDefinedFunction(boolean exported, boolean opaque, String name, List<Param> params, Maybe<AstType> ret, ArrayList<AstStmt> body)
	{
		super(exported, opaque, name, params, ret);
		this.body = body;
	}

	public ArrayList<AstStmt> body;
}
