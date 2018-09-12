// Copyright 2018 Markus Grech
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

package io.katana.compiler;

public enum Builtin
{
	ADD     ("katana.math.add"),
	SUB     ("katana.math.sub"),
	MUL     ("katana.math.mul"),
	DIV     ("katana.math.div"),
	REM     ("katana.math.rem"),
	DIV_POW2("katana.math.div_pow2"),

	NEG("katana.math.neg"),

	CMP_EQ ("katana.cmp.eq"),
	CMP_NEQ("katana.cmp.neq"),
	CMP_LT ("katana.cmp.lt"),
	CMP_LTE("katana.cmp.lte"),
	CMP_GT ("katana.cmp.gt"),
	CMP_GTE("katana.cmp.gte"),

	AND("katana.bits.and"),
	OR ("katana.bits.or"),
	XOR("katana.bits.xor"),
	SHL("katana.bits.shl"),
	SHR("katana.bits.shr"),
	ROL("katana.bits.rol"),
	ROR("katana.bits.ror"),

	NOT   ("katana.bits.not"),
	CLZ   ("katana.bits.clz"),
	CTZ   ("katana.bits.ctz"),
	POPCNT("katana.bits.popcnt"),
	BSWAP ("katana.bits.bswap"),

	MEMCPY ("katana.mem.copy"),
	MEMMOVE("katana.mem.move"),
	MEMSET ("katana.mem.set"),
	;

	public final String sourceName;

	Builtin(String sourceName)
	{
		this.sourceName = sourceName;
	}
}
