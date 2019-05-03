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

package io.katana.compiler.platform;

public enum Arch
{
	UNKNOWN("unknown", -1, -1, -1, -1, -1, -1, -1),
	AMD64  ("x86_64",   8,  8,  8,  4,  2,  4,  8),
	X86    ("x86",      4,  4,  8,  4,  2,  4,  8);

	private final String value;
	public final long pointerSize;
	public final long pointerAlign;
	public final long int64Align;
	public final long int32Align;
	public final long int16Align;
	public final long float32Align;
	public final long float64Align;

	Arch(String value, long pointerSize, long pointerAlign, long int64Align, long int32Align, long int16Align, long float32Align, long float64Align)
	{
		this.value = value;
		this.pointerSize = pointerSize;
		this.pointerAlign = pointerAlign;
		this.int64Align = int64Align;
		this.int32Align = int32Align;
		this.int16Align = int16Align;
		this.float32Align = float32Align;
		this.float64Align = float64Align;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
