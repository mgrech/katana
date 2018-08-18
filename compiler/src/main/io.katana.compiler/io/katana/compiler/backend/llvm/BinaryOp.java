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
import io.katana.compiler.BuiltinType;
import io.katana.compiler.analysis.Types;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.sema.expr.SemaExpr;
import io.katana.compiler.sema.expr.SemaExprBuiltinCall;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Maybe;

import java.util.List;

public class BinaryOp extends BuiltinFunc
{
	private Maybe<String> boolInstr;
	private Maybe<String> sintInstr;
	private Maybe<String> uintInstr;
	private Maybe<String> floatInstr;
	private Maybe<SemaType> ret;

	public BinaryOp(String name, Maybe<String> boolInstr, Maybe<String> sintInstr, Maybe<String> uintInstr, Maybe<String> floatInstr, Maybe<SemaType> ret)
	{
		super(name);
		this.boolInstr = boolInstr;
		this.sintInstr = sintInstr;
		this.uintInstr = uintInstr;
		this.floatInstr = floatInstr;
		this.ret = ret;
	}

	private String instrForType(SemaTypeBuiltin type)
	{
		if(Types.isBuiltin(type, BuiltinType.BOOL))
			return boolInstr.unwrap();

		if(Types.isSigned(type))
			return sintInstr.unwrap();

		if(Types.isUnsigned(type))
			return uintInstr.unwrap();

		if(Types.isFloatingPoint(type))
			return floatInstr.unwrap();

		return null;
	}

	@Override
	public SemaExprBuiltinCall validateCall(List<SemaExpr> args)
	{
		if(args.size() != 2)
			throw new CompileException(String.format("builtin %s expects 2 arguments", name));

		var argType = Types.removeConst(args.get(0).type());

		if(!Types.equal(argType, Types.removeConst(args.get(1).type())))
			throw new CompileException(String.format("arguments to builtin %s must be of same type", name));

		if(!Types.isBuiltin(argType))
			throw new CompileException(String.format("builtin %s requires arguments of builtin type", name));

		var instr = instrForType((SemaTypeBuiltin)argType);

		if(instr == null)
			throw new CompileException(String.format("unsupported type for builtin %s", name));

		return new SemaExprBuiltinCall(instr, args, ret.or(argType));
	}
}
