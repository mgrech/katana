// Copyright 2017 Markus Grech
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

package katana.sema.type;

import katana.analysis.StructLayout;

import java.util.List;

public class SemaTypeTuple extends SemaType
{
	public List<SemaType> types;
	public StructLayout layout;

	public SemaTypeTuple(List<SemaType> types, StructLayout layout)
	{
		this.types = types;
		this.layout = layout;
	}

	@Override
	protected boolean same(SemaType other)
	{
		if(!(other instanceof SemaTypeTuple))
			return false;

		SemaTypeTuple o = (SemaTypeTuple)other;

		if(types.size() != o.types.size())
			return false;

		for(int i = 0; i != types.size(); ++i)
			if(!types.get(i).same(o.types.get(i)))
				return false;

		return true;
	}
}
