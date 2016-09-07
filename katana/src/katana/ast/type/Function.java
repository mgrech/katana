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

package katana.ast.type;

import katana.Maybe;
import katana.ast.Type;

import java.util.ArrayList;

public class Function extends Type
{
	public Function(Maybe<Type> ret, ArrayList<Type> params)
	{
		this.ret = ret;
		this.params = params;
	}

	public Maybe<Type> ret;
	public ArrayList<Type> params;
}
