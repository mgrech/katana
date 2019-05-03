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

package io.katana.compiler.sema.decl;

import io.katana.compiler.ExportKind;
import io.katana.compiler.ast.AstPath;
import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.SemaSymbol;

public abstract class SemaDecl implements SemaSymbol
{
	private SemaModule module;
	public ExportKind exportKind;

	protected SemaDecl(SemaModule module, ExportKind exportKind)
	{
		this.module = module;
		this.exportKind = exportKind;
	}

	public AstPath qualifiedName()
	{
		var path = new AstPath();
		path.components.addAll(module.path().components);
		path.components.add(name());
		return path;
	}

	public SemaModule module()
	{
		return module;
	}
}
