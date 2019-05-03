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

package io.katana.compiler.analysis;

import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.sema.type.SemaType;

import java.util.ArrayList;
import java.util.List;

public class StructLayoutBuilder
{
	private long size = 0;
	private long alignment = 1;
	private final List<Long> fieldOffsets = new ArrayList<>();
	private final PlatformContext context;

	public StructLayoutBuilder(PlatformContext context)
	{
		this.context = context;
	}

	public StructLayoutBuilder appendField(SemaType type)
	{
		var fieldSize = Types.sizeof(type, context);
		var fieldAlignment = Types.alignof(type, context);

		size = align(size, fieldAlignment);
		fieldOffsets.add(size);
		size = size + fieldSize;
		alignment = Math.max(alignment, fieldAlignment);

		return this;
	}

	public StructLayout build()
	{
		// pad to multiple to allow putting objects into arrays
		size = align(size, alignment);
		return new StructLayout(size, alignment, fieldOffsets);
	}

	private static long align(long size, long alignment)
	{
		// (size + alignment - 1) & ~(alignment - 1)
		var alignSubOne = alignment - 1;
		var alignedSize = (size + alignSubOne) & ~alignSubOne;

		if(alignedSize < size || alignedSize % alignment != 0)
			throw new AssertionError("unreachable");

		return alignedSize;
	}
}
