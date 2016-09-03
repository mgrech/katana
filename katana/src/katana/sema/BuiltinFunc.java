package katana.sema;

import katana.Maybe;
import katana.backend.PlatformContext;
import katana.backend.llvm.FunctionContext;
import katana.sema.expr.BuiltinCall;

import java.util.List;

public abstract class BuiltinFunc
{
	public BuiltinFunc(String name)
	{
		this.name = name;
	}

	public abstract Maybe<Type> validateCall(List<Type> args);
	public abstract Maybe<String> generateCall(BuiltinCall call, StringBuilder builder, PlatformContext context, FunctionContext fcontext);

	public String name;
}
