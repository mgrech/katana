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

package katana.compiler.commands;

import katana.backend.PlatformContext;
import katana.backend.llvm.ProgramCodeGenerator;
import katana.backend.llvm.amd64.PlatformContextLlvmAmd64;
import katana.compiler.Command;
import katana.compiler.CommandException;
import katana.sema.Module;
import katana.sema.Program;
import katana.sema.ProgramValidator;
import katana.sema.decl.Decl;
import katana.sema.decl.Function;
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
public class Build
{
	private static ArrayList<Path> discoverSourceFiles(Path root) throws IOException
	{
		ArrayList<Path> paths = new ArrayList<>();

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

	private static Maybe<Decl> resolvePath(Program program, String pathString)
	{
		katana.ast.Path path = katana.ast.Path.fromString(pathString);
		int last = path.components.size() - 1;
		String symbol = path.components.get(last);
		path.components.remove(last);

		Maybe<Module> module = program.findModule(path);

		if(module.isNone())
			return Maybe.none();

		return module.unwrap().findDecl(symbol);
	}

	private static Decl findMainFunction(Program program, String name)
	{
		Maybe<Decl> main = resolvePath(program, name);

		if(main.isNone())
			throw new RuntimeException("main function not found");

		Decl decl = main.unwrap();

		if(!(decl instanceof Function))
			throw new RuntimeException("specified symbol is not a function");

		return decl;
	}

	public static void run(String[] args) throws IOException
	{
		if(args.length > 1)
			throw new CommandException("invalid number of arguments, usage: build [main-func]");

		PlatformContext context = new PlatformContextLlvmAmd64();
		List<Path> filePaths = discoverSourceFiles(Paths.get("./source"));

		Program program = ProgramValidator.validate(filePaths, context);

		Maybe<Decl> main = Maybe.none();

		if(args.length == 1)
			main = Maybe.some(findMainFunction(program, args[0]));

		String output = ProgramCodeGenerator.generate(program, context, main);

		try(OutputStream stream = new FileOutputStream("output/program.ll"))
		{
			stream.write(output.getBytes(StandardCharsets.UTF_8));
		}
	}
}
