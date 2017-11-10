// Copyright 2016-2017 Markus Grech
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

package io.katana.compiler.backend.llvm;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.analysis.Types;
import io.katana.compiler.ast.AstPath;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.TypeString;
import io.katana.compiler.project.BuildTarget;
import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.SemaProgram;
import io.katana.compiler.sema.decl.SemaDecl;
import io.katana.compiler.sema.decl.SemaDeclFunction;
import io.katana.compiler.sema.decl.SemaDeclOverloadSet;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.utils.FileUtils;
import io.katana.compiler.utils.Maybe;

import java.io.IOException;
import java.nio.file.Path;

public class ProgramCodeGenerator
{
	private static void generateDecls(DeclCodeGenerator generator, SemaModule module)
	{
		for(SemaModule child : module.children().values())
			generateDecls(generator, child);

		module.decls().values().forEach(generator::generate);
	}

	private static void generateEntryPointWrapper(StringBuilder builder, SemaDecl func)
	{
		SemaType ret = ((SemaDeclFunction)func).ret;

		builder.append("define i32 @main()\n{\n");

		if(Types.isVoid(ret))
		{
			builder.append(String.format("\tcall void @%s()\n", func.qualifiedName()));
			builder.append("\tret i32 0\n");
		}

		else
		{
			builder.append(String.format("\t%%1 = call i32 @%s()\n", func.qualifiedName()));
			builder.append("\tret i32 %1\n");
		}

		builder.append("}\n");
	}

	private static Maybe<SemaDecl> findDeclByPath(SemaProgram program, String pathString)
	{
		AstPath path = AstPath.fromString(pathString);
		int last = path.components.size() - 1;
		String symbol = path.components.get(last);
		path.components.remove(last);

		Maybe<SemaModule> module = program.findModule(path);

		if(module.isNone())
			return Maybe.none();

		return module.unwrap().findDecl(symbol);
	}

	private static SemaDecl findEntryPointFunction(SemaProgram program, String name)
	{
		Maybe<SemaDecl> entry = findDeclByPath(program, name);

		if(entry.isNone())
			throw new CompileException(String.format("entry point '%s' could not found", name));

		SemaDecl decl = entry.unwrap();

		if(!(decl instanceof SemaDeclOverloadSet))
			throw new CompileException("the specified entry point symbol does not refer to function");

		SemaDeclOverloadSet set = (SemaDeclOverloadSet)decl;

		if(set.overloads.size() != 1)
			throw new CompileException("entry point function may not be overloaded");

		SemaDeclFunction func = set.overloads.get(0);

		if(Types.isVoid(func.ret) || Types.isBuiltin(func.ret, BuiltinType.INT32))
			return func;

		throw new CompileException(String.format("entry point must return 'void' or 'int32', got '%s'", TypeString.of(func.ret)));
	}

	public static void generate(BuildTarget build, SemaProgram program, PlatformContext platform, Path outputFile) throws IOException
	{
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("target triple = \"%s\"\n\n", platform.target()));

		StringPool stringPool = new StringPool();
		FileCodegenContext context = new FileCodegenContext(build, platform, builder, stringPool);
		generateDecls(new DeclCodeGenerator(context), program.root);
		stringPool.generate(builder);

		if(build.entryPoint != null)
			generateEntryPointWrapper(builder, findEntryPointFunction(program, build.entryPoint));

		FileUtils.writeFile(builder.toString(), outputFile);
	}
}
