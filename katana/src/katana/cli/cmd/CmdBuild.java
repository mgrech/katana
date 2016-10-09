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

import katana.analysis.ProgramValidator;
import katana.ast.AstPath;
import katana.backend.PlatformContext;
import katana.backend.llvm.ProgramCodeGenerator;
import katana.backend.llvm.amd64.PlatformContextLlvmAmd64;
import katana.cli.Command;
import katana.cli.CommandException;
import katana.sema.SemaModule;
import katana.sema.SemaProgram;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclFunction;
import katana.utils.Maybe;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@Command(name = "build", desc = "builds project in working directory")
public class CmdBuild
{
	private static List<Path> discoverSourceFiles(Path root) throws IOException
	{
		List<Path> paths = new ArrayList<>();

		Files.walkFileTree(root, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
			{
				if(attrs.isRegularFile() && path.toString().endsWith(".kat"))
					paths.add(path);

				return FileVisitResult.CONTINUE;
			}
		});

		return paths;
	}

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

	private static SemaDecl findMainFunction(SemaProgram program, String name)
	{
		Maybe<SemaDecl> main = resolvePath(program, name);

		if(main.isNone())
			throw new RuntimeException("main function not found");

		SemaDecl decl = main.unwrap();

		if(!(decl instanceof SemaDeclFunction))
			throw new RuntimeException("specified symbol is not a function");

		return decl;
	}

	public static void run(String[] args) throws IOException
	{
		if(args.length > 1)
			throw new CommandException("invalid number of arguments, usage: build [main-func]");

		PlatformContext context = new PlatformContextLlvmAmd64();
		List<Path> filePaths = discoverSourceFiles(Paths.get("./source"));

		SemaProgram program = ProgramValidator.validate(filePaths, context);

		Maybe<SemaDecl> main = Maybe.none();

		if(args.length == 1)
			main = Maybe.some(findMainFunction(program, args[0]));

		String output = ProgramCodeGenerator.generate(program, context, main);

		try(OutputStream stream = new FileOutputStream("output/program.ll"))
		{
			stream.write(output.getBytes(StandardCharsets.UTF_8));
		}
	}
}
