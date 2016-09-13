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

import katana.BuiltinType;
import katana.backend.PlatformContext;
import katana.sema.Module;
import katana.sema.Program;
import katana.sema.decl.*;
import katana.sema.type.Builtin;
import katana.sema.type.Type;
import katana.utils.Maybe;

public class ProgramCodeGenerator
{
	private static void generate(DeclCodeGenerator generator, Module module)
	{
		for(Module child : module.children().values())
			generate(generator, child);

		for(Data data : module.datas().values())
			generator.generate(data);

		for(Global global : module.globals().values())
			generator.generate(global);

		for(ExternFunction externFunction : module.externFunctions().values())
			generator.generate(externFunction);

		for(Function function : module.functions().values())
			generator.generate(function);
	}

	private static void generateMainWrapper(StringBuilder builder, Decl func)
	{
		Maybe<Type> ret = func instanceof Function ? ((Function)func).ret : ((ExternFunction)func).ret;

		if(ret.isSome() && (!(ret.unwrap() instanceof Builtin) || ((Builtin)ret.unwrap()).which != BuiltinType.INT32))
			throw new RuntimeException("main function must return int32 or nothing");

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
		generate(new DeclCodeGenerator(builder, context), program.root);

		if(main.isSome())
			generateMainWrapper(builder, main.unwrap());

		return builder.toString();
	}
}
