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

package katana.sema;

import katana.ast.Path;
import katana.utils.Maybe;

public class Program
{
	public Module findOrCreateModule(Path path)
	{
		Module parent = root;

		for(String component : path.components)
			parent = parent.findOrCreateChild(component);

		return parent;
	}

	public Maybe<Module> findModule(Path path)
	{
		Module current = root;

		for(String component : path.components)
		{
			Maybe<Module> child = current.findChild(component);

			if(child.isNone())
				return child;

			current = child.get();
		}

		return Maybe.some(current);
	}

	public Module root = new Module("", new Path(), null);
}
