package katana.backend.llvm;

import katana.BuiltinType;
import katana.Maybe;
import katana.backend.PlatformContext;
import katana.sema.Decl;
import katana.sema.Module;
import katana.sema.Program;
import katana.sema.Type;
import katana.sema.decl.Data;
import katana.sema.decl.ExternFunction;
import katana.sema.decl.Function;
import katana.sema.decl.Global;
import katana.sema.type.Builtin;

public class ProgramCodeGen
{
	private static void generate(StringBuilder builder, Module module, PlatformContext context)
	{
		for(Module child : module.children().values())
			generate(builder, child, context);

		for(Data data : module.datas().values())
			DeclCodeGen.apply(data, builder, context);

		for(Global global : module.globals().values())
			DeclCodeGen.apply(global, builder, context);

		for(ExternFunction externFunction : module.externFunctions().values())
			DeclCodeGen.apply(externFunction, builder, context);

		for(Function function : module.functions().values())
			DeclCodeGen.apply(function, builder, context);
	}

	private static void generateMainWrapper(StringBuilder builder, Decl func)
	{
		Maybe<Type> ret = func instanceof Function ? ((Function)func).ret : ((ExternFunction)func).ret;

		if(ret.isSome() && (!(ret.unwrap() instanceof Builtin) || ((Builtin)ret.unwrap()).which != BuiltinType.INT32))
			throw new RuntimeException("main function must return i32 or nothing");

		builder.append("define i32 @main()\n{\n");

		if(ret.isSome())
		{
			builder.append(String.format("\t%%1 = call i32 @%s()\n", func.qualifiedName()));
			builder.append("\tret i32 %1\n");
		}

		else
		{
			builder.append(String.format("\tcall void @%s()\n", func.qualifiedName()));
			builder.append("\tret i32 0\n");
		}

		builder.append("}\n");
	}

	public static String generate(Program program, PlatformContext context, Maybe<Decl> main)
	{
		StringBuilder builder = new StringBuilder();
		generate(builder, program.root, context);

		if(main.isSome())
			generateMainWrapper(builder, main.unwrap());

		return builder.toString();
	}
}
