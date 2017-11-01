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

package io.katana.compiler.ast.decl;

import io.katana.compiler.ast.type.AstType;
import io.katana.compiler.utils.Maybe;

import java.util.List;

public class AstDeclFunction extends AstDecl
{
	public static class Param
	{
		public Param(AstType type, String name)
		{
			this.type = type;
			this.name = name;
		}

		public AstType type;
		public String name;
	}

	public String name;
	public List<Param> params;
	public Maybe<AstType> ret;

	protected AstDeclFunction(boolean exported, boolean opaque, String name, List<Param> params, Maybe<AstType> ret)
	{
		super(exported, opaque);
		this.name = name;
		this.params = params;
		this.ret = ret;
	}
}
