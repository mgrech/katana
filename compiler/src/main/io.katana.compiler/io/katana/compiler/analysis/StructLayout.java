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

import java.util.List;

public class StructLayout
{
	private final long size;
	private final long alignment;
	private final List<Long> fieldOffsets;

	public StructLayout(long size, long alignment, List<Long> fieldOffsets)
	{
		this.size = size;
		this.alignment = alignment;
		this.fieldOffsets = fieldOffsets;
	}

	public long sizeof()
	{
		return size;
	}

	public long alignof()
	{
		return alignment;
	}

	public long offsetof(int index)
	{
		return fieldOffsets.get(index);
	}
}
