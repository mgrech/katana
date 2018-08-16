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
import io.katana.compiler.backend.llvm.ir.IrValueSsa;
import io.katana.compiler.diag.CompileException;
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

	private void unsupportedType(String what)
	{
		throw new CompileException(String.format("builtin %s does not support %s", name, what));
	}

	private void checkTypeSupport(BuiltinType type)
	{
		switch(type.kind)
		{
		case BOOL:
			if(boolInstr.isNone())
				unsupportedType("bools");
			break;

		case INT:
			if(sintInstr.isNone())
				unsupportedType("signed integer types");
			break;

		case UINT:
			if(uintInstr.isNone())
				unsupportedType("unsigned integer types");
			break;

		case FLOAT:
			if(floatInstr.isNone())
				unsupportedType("floating point types");
			break;

		default: throw new AssertionError("unreachable");
		}
	}

	@Override
	public SemaType validateCall(List<SemaType> args)
	{
		if(args.size() != 2)
			throw new CompileException(String.format("builtin %s expects 2 arguments", name));

		var argType = Types.removeConst(args.get(0));

		if(!Types.equal(argType, Types.removeConst(args.get(1))))
			throw new CompileException(String.format("arguments to builtin %s must be of same type", name));

		if(!Types.isBuiltin(argType))
			throw new CompileException(String.format("builtin %s requires arguments of builtin type", name));

		checkTypeSupport(((SemaTypeBuiltin)argType).which);
		return ret.or(argType);
	}

	private String instrForType(SemaTypeBuiltin type)
	{
		switch(type.which.kind)
		{
		case BOOL: return boolInstr.unwrap();
		case INT: return sintInstr.unwrap();
		case UINT: return uintInstr.unwrap();
		case FLOAT: return floatInstr.unwrap();
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	@Override
	public IrValueSsa generateCall(SemaExprBuiltinCall call, FunctionCodegenContext context)
	{
		var type = (SemaTypeBuiltin)call.args.get(0).type();
		var typeIr = TypeCodeGenerator.generate(type, context.platform());
		var left = ExprCodeGenerator.generate(call.args.get(0), context).unwrap();
		var right = ExprCodeGenerator.generate(call.args.get(1), context).unwrap();
		var instr = instrForType(type);
		var result = context.allocateSsa();
		context.writef("\t%s = %s %s %s, %s\n", result, instr, typeIr, left, right);
		return result;
	}
}
