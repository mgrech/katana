// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.backend.llvm;

import io.katana.compiler.BuiltinFunc;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Maybe;

import java.util.HashMap;
import java.util.Map;

public class PlatformContextLlvm extends PlatformContext
{
	private static final Map<String, BuiltinFunc> BUILTINS = new HashMap<>();

	private static void registerBinaryOp(String name, String boolInstr, String sintInstr, String uintInstr, String floatInstr, SemaTypeBuiltin ret)
	{
		BUILTINS.put(name, new BinaryOp(name, Maybe.wrap(boolInstr), Maybe.wrap(sintInstr), Maybe.wrap(uintInstr), Maybe.wrap(floatInstr), Maybe.wrap(ret)));
	}

	static
	{
		registerBinaryOp("eq",   "icmp eq", "icmp eq",  "icmp eq",  "fcmp oeq", SemaTypeBuiltin.BOOL);
		registerBinaryOp("neq",  "icmp ne", "icmp ne",  "icmp ne",  "fcmp one", SemaTypeBuiltin.BOOL);
		registerBinaryOp("lt",   null,      "icmp slt", "icmp ult", "fcmp olt", SemaTypeBuiltin.BOOL);
		registerBinaryOp("lteq", null,      "icmp sle", "icmp ule", "fcmp ole", SemaTypeBuiltin.BOOL);
		registerBinaryOp("gt",   null,      "icmp sgt", "icmp ugt", "fcmp ogt", SemaTypeBuiltin.BOOL);
		registerBinaryOp("gteq", null,      "icmp sge", "icmp uge", "fcmp oge", SemaTypeBuiltin.BOOL);

		registerBinaryOp("add", null, "add",  "add",  "fadd", null);
		registerBinaryOp("sub", null, "sub",  "sub",  "fsub", null);
		registerBinaryOp("mul", null, "mul",  "mul",  "fmul", null);
		registerBinaryOp("div", null, "sdiv", "udiv", "fdiv", null);
		registerBinaryOp("mod", null, "srem", "urem", "frem", null);

		registerBinaryOp("shl", null,  "shl",  "shl",  null, null);
		registerBinaryOp("shr", null,  "lshr", "lshr", null, null);
		registerBinaryOp("and", "and", "and",  "and",  null, null);
		registerBinaryOp("or",  "or",  "or",   "or",   null, null);
		registerBinaryOp("xor", "xor", "xor",  "xor",  null, null);
	}

	public PlatformContextLlvm(TargetTriple triple)
	{
		super(triple);
	}

	@Override
	public Maybe<BuiltinFunc> findBuiltin(String name)
	{
		BuiltinFunc func = BUILTINS.get(name);
		return Maybe.wrap(func);
	}
}
