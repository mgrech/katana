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

package katana.sema.decl;

import katana.ast.AstPath;
import katana.sema.SemaModule;
import katana.sema.SemaSymbol;
import katana.visitor.IVisitable;

public abstract class SemaDecl implements SemaSymbol, IVisitable
{
	protected SemaDecl(SemaModule module, boolean exported, boolean opaque)
	{
		this.module = module;
		this.exported = exported;
		this.opaque = opaque;
	}

	public AstPath qualifiedName()
	{
		AstPath path = new AstPath();
		path.components.addAll(module.path().components);
		path.components.add(name());
		return path;
	}

	public SemaModule module()
	{
		return module;
	}

	private SemaModule module;
	public boolean exported;
	public boolean opaque;
}
