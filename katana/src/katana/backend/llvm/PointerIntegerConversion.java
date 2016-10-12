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
import katana.sema.expr.SemaExpr;
import katana.sema.expr.SemaExprBuiltinCall;
import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeBuiltin;
import katana.utils.Maybe;

import java.util.List;

public class PointerIntegerConversion extends BuiltinFunc
{
	enum Which
	{
		PTR2PINT,
		PTR2UPINT,
		INT2PTR,
	}

	public PointerIntegerConversion(String name, Which which)
	{
		super(name);
		this.which = which;
	}

	@Override
	public SemaType validateCall(List<SemaType> args)
	{
		if(args.size() != 1)
			throw new RuntimeException(String.format("builtin %s expects exactly 1 argument", name));

		SemaType arg = args.get(0);

		switch(which)
		{
		case PTR2PINT:
		case PTR2UPINT:
			if(!SemaType.same(arg, SemaTypeBuiltin.PTR))
				throw new RuntimeException(String.format("builtin %s expects argument of type ptr", name));

			return which == Which.PTR2PINT ? SemaTypeBuiltin.PINT : SemaTypeBuiltin.UPINT;

		case INT2PTR:
			if(!SemaType.same(arg, SemaTypeBuiltin.UPINT) && !SemaType.same(arg, SemaTypeBuiltin.PINT))
				throw new RuntimeException(String.format("builtin %s expects argument of type pint or upint", name));

			return SemaTypeBuiltin.PTR;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	@Override
	public Maybe<String> generateCall(SemaExprBuiltinCall call, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		switch(which)
		{
		case PTR2PINT:
		case PTR2UPINT:
			{
				String argSSA = ExprCodeGenerator.generate(call.args.get(0), builder, context, fcontext).unwrap();
				String resultTypeString = TypeCodeGenerator.generate(which == Which.PTR2PINT ? SemaTypeBuiltin.PINT : SemaTypeBuiltin.UPINT, context);
				String resultSSA = fcontext.allocateSSA();
				builder.append(String.format("\t%s = ptrtoint i8* %s to %s\n", resultSSA, argSSA, resultTypeString));
				return Maybe.some(resultSSA);
			}

		case INT2PTR:
			{
				SemaExpr arg = call.args.get(0);
				String argSSA = ExprCodeGenerator.generate(arg, builder, context, fcontext).unwrap();
				String argTypeString = TypeCodeGenerator.generate(arg.type(), context);
				String resultSSA = fcontext.allocateSSA();
				builder.append(String.format("\t%s = inttoptr %s %s to i8*\n", resultSSA, argTypeString, argSSA));
				return Maybe.some(resultSSA);
			}

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private Which which;
}
