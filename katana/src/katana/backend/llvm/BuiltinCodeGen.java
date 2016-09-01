package katana.backend.llvm;

import katana.backend.PlatformContext;
import katana.sema.Type;
import katana.sema.expr.BuiltinCall;

public class BuiltinCodeGen
{
	public static String apply(BuiltinCall call, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		switch(call.func.name.toString())
		{
		case "std.add.i":
			{
				Type resultType = call.func.params.get(0);
				String typeString = TypeCodeGen.apply(resultType, context);
				String leftSSA = ExprCodeGen.apply(call.args.get(0), builder, context, fcontext).unwrap();
				String rightSSA = ExprCodeGen.apply(call.args.get(1), builder, context, fcontext).unwrap();
				String resultSSA = fcontext.allocateSSA();
				builder.append(String.format("\t%s = add %s %s, %s\n", resultSSA, typeString, leftSSA, rightSSA));
				return resultSSA;
			}

		case "std.less.i":
			{
				Type resultType = call.func.params.get(0);
				String typeString = TypeCodeGen.apply(resultType, context);
				String leftSSA = ExprCodeGen.apply(call.args.get(0), builder, context, fcontext).unwrap();
				String rightSSA = ExprCodeGen.apply(call.args.get(1), builder, context, fcontext).unwrap();
				String resultSSA = fcontext.allocateSSA();
				builder.append(String.format("\t%s = icmp slt %s %s, %s\n", resultSSA, typeString, leftSSA, rightSSA));
				return resultSSA;
			}

		default: break;
		}

		throw new RuntimeException("codegen: unknown builtin " + call.func.name.toString());
	}
}
