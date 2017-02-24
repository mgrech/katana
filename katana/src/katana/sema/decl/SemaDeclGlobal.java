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

package katana.sema.decl;

import katana.sema.SemaModule;
import katana.sema.expr.SemaExpr;
import katana.sema.type.SemaType;
import katana.utils.Maybe;

public class SemaDeclGlobal extends SemaDecl
{
	public String name;
	public SemaType type;
	public Maybe<SemaExpr> init;

	public SemaDeclGlobal(SemaModule module, boolean exported, boolean opaque, String name)
	{
		super(module, exported, opaque);
		this.name = name;
	}

	@Override
	public String name()
	{
		return name;
	}
}
