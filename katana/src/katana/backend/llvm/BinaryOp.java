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
import katana.BuiltinType;
import katana.backend.PlatformContext;
import katana.sema.expr.SemaExprBuiltinCall;
import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeBuiltin;
import katana.utils.Maybe;

import java.util.List;

public class BinaryOp extends BuiltinFunc
{
	public BinaryOp(String name, Maybe<String> boolInstr, Maybe<String> sintInstr, Maybe<String> uintInstr, Maybe<String> floatInstr, Maybe<String> ptrInstr, Maybe<SemaType> ret)
	{
		super(name);
		this.boolInstr = boolInstr;
		this.sintInstr = sintInstr;
		this.uintInstr = uintInstr;
		this.floatInstr = floatInstr;
		this.ptrInstr = ptrInstr;
		this.ret = ret;
	}

	private void unsupportedType(String what)
	{
		throw new RuntimeException(String.format("builtin %s does not support %s", name, what));
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

		case PTR:
			if(ptrInstr.isNone())
				unsupportedType("pointers");
			break;

		default: throw new AssertionError("unreachable");
		}
	}

	@Override
	public Maybe<SemaType> validateCall(List<SemaType> args)
	{
		if(args.size() != 2)
			throw new RuntimeException(String.format("builtin %s expects 2 arguments", name));

		SemaType argType = args.get(0);

		if(!SemaType.same(argType, args.get(1)))
			throw new RuntimeException(String.format("arguments to builtin %s must be of same type", name));

		if(!(argType instanceof SemaTypeBuiltin))
			throw new RuntimeException(String.format("builtin %s requires arguments of builtin type", name));

		checkTypeSupport(((SemaTypeBuiltin)argType).which);
		return Maybe.some(ret.or(argType));
	}

	private String instrForType(SemaTypeBuiltin type)
	{
		switch(type.which.kind)
		{
		case BOOL: return boolInstr.unwrap();
		case INT: return sintInstr.unwrap();
		case UINT: return uintInstr.unwrap();
		case FLOAT: return floatInstr.unwrap();
		case PTR: return ptrInstr.unwrap();
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	@Override
	public Maybe<String> generateCall(SemaExprBuiltinCall call, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		SemaTypeBuiltin type = (SemaTypeBuiltin)call.args.get(0).type().unwrap();
		String typeString = TypeCodeGenerator.generate(type, context);
		String leftSSA = ExprCodeGenerator.generate(call.args.get(0), builder, context, fcontext).unwrap();
		String rightSSA = ExprCodeGenerator.generate(call.args.get(1), builder, context, fcontext).unwrap();
		String instr = instrForType(type);
		String resultSSA = fcontext.allocateSSA();
		builder.append(String.format("\t%s = %s %s %s, %s\n", resultSSA, instr, typeString, leftSSA, rightSSA));
		return Maybe.some(resultSSA);
	}

	private Maybe<String> boolInstr;
	private Maybe<String> sintInstr;
	private Maybe<String> uintInstr;
	private Maybe<String> floatInstr;
	private Maybe<String> ptrInstr;
	private Maybe<SemaType> ret;
}
