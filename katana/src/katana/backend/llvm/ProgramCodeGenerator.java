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
import katana.sema.SemaModule;
import katana.sema.SemaProgram;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclFunction;
import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeBuiltin;
import katana.utils.Maybe;

public class ProgramCodeGenerator
{
	private static void generate(DeclCodeGenerator generator, SemaModule module)
	{
		for(SemaModule child : module.children().values())
			generate(generator, child);

		for(SemaDecl decl : module.decls().values())
			generator.generate(decl);
	}

	private static void generateEntryPointWrapper(StringBuilder builder, SemaDecl func)
	{
		Maybe<SemaType> ret = ((SemaDeclFunction)func).ret;

		if(ret.isSome() && (!(ret.unwrap() instanceof SemaTypeBuiltin) || ((SemaTypeBuiltin)ret.unwrap()).which != BuiltinType.INT32))
			throw new AssertionError("unreachable");

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

	public static String generate(SemaProgram program, PlatformContext context, Maybe<SemaDecl> entry)
	{
		StringBuilder builder = new StringBuilder();
		generate(new DeclCodeGenerator(builder, context), program.root);

		if(entry.isSome())
			generateEntryPointWrapper(builder, entry.unwrap());

		return builder.toString();
	}
}
