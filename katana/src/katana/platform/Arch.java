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

package katana.platform;

import java.math.BigInteger;

public enum Arch
{
	UNKNOWN("unknown", -1, -1, -1, -1, -1, -1, -1),
	AMD64  ("x86_64",   8,  8,  8,  4,  2,  4,  8),
	X86    ("x86",      4,  4,  8,  4,  2,  4,  8);

	private final String value;
	public final BigInteger pointerSize;
	public final BigInteger pointerAlign;
	public final BigInteger int64Align;
	public final BigInteger int32Align;
	public final BigInteger int16Align;
	public final BigInteger float32Align;
	public final BigInteger float64Align;

	Arch(String value, int pointerSize, int pointerAlign, int int64Align, int int32Align, int int16Align, int float32Align, int float64Align)
	{
		this.value = value;
		this.pointerSize = BigInteger.valueOf(pointerSize);
		this.pointerAlign = BigInteger.valueOf(pointerAlign);
		this.int64Align = BigInteger.valueOf(int64Align);
		this.int32Align = BigInteger.valueOf(int32Align);
		this.int16Align = BigInteger.valueOf(int16Align);
		this.float32Align = BigInteger.valueOf(float32Align);
		this.float64Align = BigInteger.valueOf(float64Align);
	}

	@Override
	public String toString()
	{
		return value;
	}
}
