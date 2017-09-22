// Copyright 2016-2017 Markus Grech
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

package katana.analysis;

import java.math.BigInteger;
import java.util.List;

public class StructLayout
{
	private final BigInteger size;
	private final BigInteger alignment;
	private final List<BigInteger> fieldOffsets;

	public StructLayout(BigInteger size, BigInteger alignment, List<BigInteger> fieldOffsets)
	{
		this.size = size;
		this.alignment = alignment;
		this.fieldOffsets = fieldOffsets;
	}

	public BigInteger sizeof()
	{
		return size;
	}

	public BigInteger alignof()
	{
		return alignment;
	}

	public BigInteger offsetof(int index)
	{
		return fieldOffsets.get(index);
	}
}