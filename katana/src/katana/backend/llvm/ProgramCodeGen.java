package katana.backend.llvm;

import katana.backend.PlatformContext;
import katana.sema.Module;
import katana.sema.Program;
import katana.sema.decl.Data;
import katana.sema.decl.ExternFunction;
import katana.sema.decl.Function;
import katana.sema.decl.Global;

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

	public static String generate(Program program, PlatformContext context)
	{
		StringBuilder builder = new StringBuilder();
		generate(builder, program.root, context);
		return builder.toString();
	}
}
