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
				Type type = call.func.params.get(0);
				String typeString = TypeCodeGen.apply(type, context);
				String left = ExprCodeGen.apply(call.args.get(0), builder, context, fcontext);
				String right = ExprCodeGen.apply(call.args.get(1), builder, context, fcontext);
				int tmp = fcontext.nextTemporary();
				builder.append(String.format("\t%%%s = add %s %s, %s\n", tmp, typeString, left, right));
				return "%" + tmp;
			}

		case "std.less.i":
			{
				Type type = call.func.params.get(0);
				String typeString = TypeCodeGen.apply(type, context);
				String left = ExprCodeGen.apply(call.args.get(0), builder, context, fcontext);
				String right = ExprCodeGen.apply(call.args.get(1), builder, context, fcontext);
				int tmp = fcontext.nextTemporary();
				builder.append(String.format("\t%%%s = icmp slt %s %s, %s\n", tmp, typeString, left, right));
				return "%" + tmp;
			}

		default: break;
		}

		throw new RuntimeException("codegen: unknown builtin " + call.func.name.toString());
	}
}
