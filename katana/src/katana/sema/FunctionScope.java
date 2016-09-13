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

import katana.sema.decl.Function;

import java.util.Collections;
import java.util.List;

public class FunctionScope implements Scope
{
	public FunctionScope(FileScope parent, Function function)
	{
		this.parent = parent;
		this.function = function;
	}

	@Override
	public List<Symbol> find(String name)
	{
		Function.Local local = function.localsByName.get(name);

		if(local != null)
			return Collections.singletonList(local);

		Function.Param param = function.paramsByName.get(name);

		if(param != null)
			return Collections.singletonList(param);

		return parent.find(name);
	}

	private FileScope parent;
	private Function function;
}
