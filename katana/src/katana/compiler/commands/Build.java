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

import katana.utils.Maybe;
import katana.ast.File;
import katana.backend.PlatformContext;
import katana.backend.llvm.ProgramCodeGenerator;
import katana.backend.llvm.amd64.PlatformContextLlvmAmd64;
import katana.compiler.Command;
import katana.compiler.CommandException;
import katana.parser.FileParser;
import katana.sema.FileValidator;
import katana.sema.Module;
import katana.sema.Program;
import katana.sema.decl.ExternFunction;
import katana.sema.decl.Function;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	private static void validateImports(Program program, Set<katana.ast.Path> imports)
	{
		for(katana.ast.Path path : imports)
			if(program.findModule(path).isNone())
				throw new RuntimeException("import of unknown module '" + path + "'");
	}

	private static katana.sema.Decl findMainFunction(Program program, String name)
	{
		String[] pathComponents = name.split("\\.");
		String function = pathComponents[pathComponents.length - 1];

		List<String> modulePath = new ArrayList<>();

		for(int i = 0; i != pathComponents.length - 1; ++i)
			modulePath.add(pathComponents[i]);

		katana.ast.Path path = new katana.ast.Path(modulePath);
		Maybe<Module> maybeModule = program.findModule(path);

		if(maybeModule.isNone())
			throw new RuntimeException("main function not found -- unknown module");

		Maybe<katana.sema.Decl> maybeDecl = maybeModule.unwrap().findSymbol(function);

		if(maybeDecl.isNone())
			throw new RuntimeException("main function not found -- unknown symbol");

		katana.sema.Decl decl = maybeDecl.unwrap();

		if(!(decl instanceof Function) && !(decl instanceof ExternFunction))
			throw new RuntimeException("specified symbol is not a function");

		return decl;
	}

	public static void run(String[] args) throws IOException
	{
		if(args.length > 1)
			throw new CommandException("invalid number of arguments, usage: build [main-func]");

		PlatformContext context = new PlatformContextLlvmAmd64();
		Program program = new Program();

		ArrayList<Path> paths = discoverSourceFiles(Paths.get("./source"));
		Set<katana.ast.Path> imports = new HashSet<>();

		for(Path path : paths)
		{
			File file = FileParser.parse(path);
			FileValidator validator = new FileValidator(context, program);
			validator.validate(file);
			validator.finalizeValidation();
			imports.addAll(validator.imports());
		}

		validateImports(program, imports);

		Maybe<katana.sema.Decl> main = Maybe.none();

		if(args.length == 1)
			main = Maybe.some(findMainFunction(program, args[0]));

		String output = ProgramCodeGenerator.generate(program, context, main);

		try(OutputStream stream = new FileOutputStream("output/program.ll"))
		{
			stream.write(output.getBytes(StandardCharsets.UTF_8));
		}
	}
}
