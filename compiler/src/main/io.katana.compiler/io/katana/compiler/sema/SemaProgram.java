// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.sema;

import io.katana.compiler.ast.AstPath;
import io.katana.compiler.utils.Maybe;

public class SemaProgram
{
	public SemaModule root = new SemaModule("", new AstPath(), null);

	public SemaModule findOrCreateModule(AstPath path)
	{
		var parent = root;

		for(var component : path.components)
			parent = parent.findOrCreateChild(component);

		return parent;
	}

	public Maybe<SemaModule> findModule(AstPath path)
	{
		var current = root;

		for(var component : path.components)
		{
			var child = current.findChild(component);

			if(child.isNone())
				return child;

			current = child.get();
		}

		return Maybe.some(current);
	}
}
