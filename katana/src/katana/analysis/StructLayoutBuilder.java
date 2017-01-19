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

import katana.backend.PlatformContext;
import katana.sema.type.SemaType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class StructLayoutBuilder
{
	private BigInteger size = BigInteger.ZERO;
	private BigInteger alignment = BigInteger.ONE;
	private final List<BigInteger> fieldOffsets = new ArrayList<>();
	private final PlatformContext context;

	public StructLayoutBuilder(PlatformContext context)
	{
		this.context = context;
	}

	public StructLayoutBuilder appendField(SemaType type)
	{
		BigInteger fieldSize = TypeSize.of(type, context);
		BigInteger fieldAlignment = TypeAlignment.of(type, context);

		size = align(size, fieldAlignment);
		fieldOffsets.add(size);
		size = size.add(fieldSize);
		alignment = alignment.max(fieldAlignment);

		return this;
	}

	public StructLayout build()
	{
		// pad to multiple to allow putting objects into arrays
		size = align(size, alignment);
		return new StructLayout(size, alignment, fieldOffsets);
	}

	private static BigInteger align(BigInteger size, BigInteger alignment)
	{
		// (size + alignment - 1) & ~(alignment - 1)
		BigInteger alignSubOne = alignment.subtract(BigInteger.ONE);
		BigInteger alignedSize = size.add(alignSubOne).and(alignSubOne.not());

		// alignedSize < size || alignedSize % alignment != 0
		if(alignedSize.compareTo(size) == -1 || !alignedSize.mod(alignment).equals(BigInteger.ZERO))
			throw new AssertionError("unreachable");

		return alignedSize;
	}
}
