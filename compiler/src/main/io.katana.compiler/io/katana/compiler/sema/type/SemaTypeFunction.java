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

package io.katana.compiler.sema.type;

import java.util.List;

public class SemaTypeFunction extends SemaType
{
	public static class ParamList
	{
		public List<SemaType> fixedParamTypes;
		public boolean isVariadic;

		public ParamList(List<SemaType> fixedParamTypes, boolean isVariadic)
		{
			this.fixedParamTypes = fixedParamTypes;
			this.isVariadic = isVariadic;
		}
	}

	public ParamList params;
	public SemaType returnType;

	public SemaTypeFunction(ParamList params, SemaType returnType)
	{
		this.params = params;
		this.returnType = returnType;
	}
}
