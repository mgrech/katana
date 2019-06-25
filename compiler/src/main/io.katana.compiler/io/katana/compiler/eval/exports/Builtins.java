// Copyright 2019 Markus Grech
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

package io.katana.compiler.eval.exports;

@SuppressWarnings("unused")
public class Builtins
{
	public static byte clz(byte b)
	{
		return (byte)(clz(b & 0xFF) - 24);
	}

	public static short clz(short s)
	{
		return (short)(clz(s & 0xFFFF) - 16);
	}

	public static int clz(int i)
	{
		return Integer.numberOfLeadingZeros(i);
	}

	public static long clz(long l)
	{
		return Long.numberOfLeadingZeros(l);
	}

	public static byte ctz(byte b)
	{
		return (byte)ctz((int)b);
	}

	public static short ctz(short s)
	{
		return (short)ctz((int)s);
	}

	public static int ctz(int i)
	{
		return Integer.numberOfTrailingZeros(i);
	}

	public static long ctz(long l)
	{
		return Long.numberOfTrailingZeros(l);
	}

	public static byte popcnt(byte b)
	{
		return (byte)popcnt((int)b);
	}

	public static short popcnt(short s)
	{
		return (short)popcnt((int)s);
	}

	public static int popcnt(int i)
	{
		return Integer.bitCount(i);
	}

	public static long popcnt(long l)
	{
		return Long.bitCount(l);
	}
}
