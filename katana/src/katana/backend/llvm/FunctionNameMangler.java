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

package katana.backend.llvm;

import katana.sema.decl.SemaDeclFunction;

public class FunctionNameMangler
{
	public static String mangle(SemaDeclFunction function)
	{
		StringBuilder builder = new StringBuilder();

		for(SemaDeclFunction.Param param : function.params)
		{
			builder.append('$');
			builder.append(TypeMangler.mangle(param.type));
		}

		return function.qualifiedName().toString() + builder.toString();
	}
}