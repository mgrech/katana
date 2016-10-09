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

package katana.backend.llvm;

import katana.BuiltinFunc;
import katana.backend.PlatformContext;
import katana.sema.type.SemaTypeBuiltin;
import katana.utils.Maybe;

import java.util.HashMap;
import java.util.Map;

public abstract class PlatformContextLlvm implements PlatformContext
{
	private static final Map<String, BuiltinFunc> BUILTINS = new HashMap<>();

	private static void registerBinaryOp(String name, String boolInstr, String sintInstr, String uintInstr, String floatInstr, String ptrInstr, SemaTypeBuiltin ret)
	{
		BUILTINS.put(name, new BinaryOp(name, Maybe.wrap(boolInstr), Maybe.wrap(sintInstr), Maybe.wrap(uintInstr), Maybe.wrap(floatInstr), Maybe.wrap(ptrInstr), Maybe.wrap(ret)));
	}

	private static void registerPointerIntegerConversion(String name, PointerIntegerConversion.Which which)
	{
		BUILTINS.put(name, new PointerIntegerConversion(name, which));
	}

	static
	{
		registerBinaryOp("eq",   "icmp eq", "icmp eq",  "icmp eq",  "fcmp oeq", "icmp eq", SemaTypeBuiltin.BOOL);
		registerBinaryOp("neq",  "icmp ne", "icmp ne",  "icmp ne",  "fcmp one", "icmp ne", SemaTypeBuiltin.BOOL);
		registerBinaryOp("lt",   null,      "icmp slt", "icmp ult", "fcmp olt", null,      SemaTypeBuiltin.BOOL);
		registerBinaryOp("lteq", null,      "icmp sle", "icmp ule", "fcmp ole", null,      SemaTypeBuiltin.BOOL);
		registerBinaryOp("gt",   null,      "icmp sgt", "icmp ugt", "fcmp ogt", null,      SemaTypeBuiltin.BOOL);
		registerBinaryOp("gteq", null,      "icmp sge", "icmp uge", "fcmp oge", null,      SemaTypeBuiltin.BOOL);

		registerBinaryOp("add", null, "add",  "add",  "fadd", null, null);
		registerBinaryOp("sub", null, "sub",  "sub",  "fsub", null, null);
		registerBinaryOp("mul", null, "mul",  "mul",  "fmul", null, null);
		registerBinaryOp("div", null, "sdiv", "udiv", "fdiv", null, null);
		registerBinaryOp("mod", null, "srem", "urem", "frem", null, null);

		registerBinaryOp("shl", null,  "shl",  "shl",  null, null, null);
		registerBinaryOp("shr", null,  "lshr", "lshr", null, null, null);
		registerBinaryOp("and", "and", "and",  "and",  null, null, null);
		registerBinaryOp("or",  "or",  "or",   "or",   null, null, null);
		registerBinaryOp("xor", "xor", "xor",  "xor",  null, null, null);

		registerPointerIntegerConversion("ptrtou", PointerIntegerConversion.Which.PTR2UPINT);
		registerPointerIntegerConversion("ptrtoi", PointerIntegerConversion.Which.PTR2PINT);
		registerPointerIntegerConversion("inttoptr", PointerIntegerConversion.Which.INT2PTR);
	}

	@Override
	public Maybe<BuiltinFunc> findBuiltin(String name)
	{
		BuiltinFunc func = BUILTINS.get(name);
		return Maybe.wrap(func);
	}
}
