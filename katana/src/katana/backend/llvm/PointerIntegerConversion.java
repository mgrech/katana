package katana.backend.llvm;

import katana.Maybe;
import katana.backend.PlatformContext;
import katana.sema.BuiltinFunc;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.expr.BuiltinCall;
import katana.sema.type.Builtin;

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
	public Maybe<Type> validateCall(List<Type> args)
	{
		if(args.size() != 1)
			throw new RuntimeException(String.format("builtin %s expects exactly 1 argument", name));

		Type arg = args.get(0);

		switch(which)
		{
		case PTR2PINT:
		case PTR2UPINT:
			if(!Type.same(arg, Builtin.PTR))
				throw new RuntimeException(String.format("builtin %s expects argument of type ptr", name));

			return Maybe.some(which == Which.PTR2PINT ? Builtin.PINT : Builtin.UPINT);

		case INT2PTR:
			if(!Type.same(arg, Builtin.UPINT) && !Type.same(arg, Builtin.PINT))
				throw new RuntimeException(String.format("builtin %s expects argument of type pint or upint", name));

			return Maybe.some(Builtin.PTR);

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	@Override
	public Maybe<String> generateCall(BuiltinCall call, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		switch(which)
		{
		case PTR2PINT:
		case PTR2UPINT:
			{
				String argSSA = ExprCodeGen.apply(call.args.get(0), builder, context, fcontext).unwrap();
				String resultTypeString = TypeCodeGen.apply(which == Which.PTR2PINT ? Builtin.PINT : Builtin.UPINT, context);
				String resultSSA = fcontext.allocateSSA();
				builder.append(String.format("\t%s = ptrtoint i8* %s to %s\n", resultSSA, argSSA, resultTypeString));
				return Maybe.some(resultSSA);
			}

		case INT2PTR:
			{
				Expr arg = call.args.get(0);
				String argSSA = ExprCodeGen.apply(arg, builder, context, fcontext).unwrap();
				String argTypeString = TypeCodeGen.apply(arg.type().unwrap(), context);
				String resultSSA = fcontext.allocateSSA();
				builder.append(String.format("\t%s = inttoptr %s %s to i8*\n", resultSSA, argTypeString, argSSA));
				return Maybe.some(fcontext.allocateSSA());
			}

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private Which which;
}
