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

import katana.ast.Decl;
import katana.ast.Path;
import katana.utils.Maybe;

public class Import extends Decl
{
	public Import(Path path, Maybe<String> rename)
	{
		super(false, false);
		this.path = path;
		this.rename = rename;
	}

	public Path path;
	public Maybe<String> rename;

	@Override
	public String toString()
	{
		String rename = this.rename.or("<none>");
		return String.format("%s\tmodule: %s\n\trename: %s\n", super.toString(), path, rename);
	}
}
