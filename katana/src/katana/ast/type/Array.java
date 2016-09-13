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

public class Array extends Type
{
	public Array(int size, Type type)
	{
		this.size = size;
		this.type = type;
	}

	public int size;
	public Type type;

	@Override
	public String toString()
	{
		return String.format("[%s]%s", size, type);
	}
}
