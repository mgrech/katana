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

package katana.cli.cmd;

import katana.BuiltinType;
import katana.analysis.ProgramValidator;
import katana.analysis.TypeHelper;
import katana.ast.AstPath;
import katana.ast.AstProgram;
import katana.backend.PlatformContext;
import katana.backend.llvm.ProgramCodeGenerator;
import katana.backend.llvm.amd64.PlatformContextLlvmAmd64;
import katana.cli.Command;
import katana.cli.CommandException;
import katana.diag.CompileException;
import katana.diag.TypeString;
import katana.parser.ProgramParser;
import katana.project.ProjectConfig;
import katana.project.ProjectManager;
import katana.sema.SemaModule;
import katana.sema.SemaProgram;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclFunction;
import katana.sema.decl.SemaDeclOverloadSet;
import katana.utils.Maybe;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@Command(name = "build", desc = "builds project in working directory")
public class CmdBuild
{
	private static Maybe<SemaDecl> resolvePath(SemaProgram program, String pathString)
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
		Maybe<SemaDecl> entry = resolvePath(program, name);

		if(entry.isNone())
			throw new CompileException(String.format("entry point '%s' could not found", name));

		SemaDecl decl = entry.unwrap();

		if(!(decl instanceof SemaDeclOverloadSet))
			throw new CompileException("the specified entry point symbol does not refer to function");

		SemaDeclOverloadSet set = (SemaDeclOverloadSet)decl;

		if(set.overloads.size() != 1)
			throw new CompileException("entry point function may not be overloaded");

		SemaDeclFunction func = set.overloads.get(0);

		if(TypeHelper.isVoidType(func.ret) || TypeHelper.isBuiltinType(func.ret, BuiltinType.INT32))
			return func;

		throw new CompileException(String.format("entry point must return 'void' or 'int32', got '%s'", TypeString.of(func.ret)));
	}

	public static void run(String[] args) throws IOException
	{
		if(args.length != 1)
			throw new CommandException("invalid number of arguments, usage: build <source-dir>");

		Path root = Paths.get(args[0]).toAbsolutePath().normalize();
		ProjectConfig config = ProjectManager.load(root);
		PlatformContext context = new PlatformContextLlvmAmd64();

		try
		{
			AstProgram ast = ProgramParser.parse(root, config);
			SemaProgram program = ProgramValidator.validate(ast, context);

			Maybe<SemaDecl> entry = Maybe.none();

			if(config.entryPoint != null)
				entry = Maybe.some(findEntryPointFunction(program, config.entryPoint));

			String output = ProgramCodeGenerator.generate(program, context, entry);

			try(OutputStream stream = new FileOutputStream("program.ll"))
			{
				stream.write(output.getBytes(StandardCharsets.UTF_8));
			}
		}

		catch(CompileException e)
		{
			System.err.println(e.getMessage());
		}
	}
}
