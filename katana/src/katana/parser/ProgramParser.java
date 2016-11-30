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

package katana.parser;

import katana.Katana;
import katana.ast.AstFile;
import katana.ast.AstProgram;
import katana.project.ProjectConfig;
import katana.scanner.Scanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class ProgramParser
{
	public static AstProgram parse(Path root, ProjectConfig config) throws IOException
	{
		List<Path> files = discoverSourceFiles(root, config);
		AstProgram program = new AstProgram();

		for(Path path : files)
		{
			byte[] data = Files.readAllBytes(path);
			int[] codepoints = new String(data, StandardCharsets.UTF_8).codePoints().toArray();

			Scanner scanner = new Scanner(path, codepoints);
			scanner.advance();

			AstFile file = FileParser.parse(scanner);
			program.files.put(path, file);
		}

		return program;
	}

	private static List<Path> discoverSourceFiles(Path root, ProjectConfig config) throws IOException
	{
		List<Path> paths = new ArrayList<>();

		for(String pathString : config.sourcePaths)
		{
			Path path = root.resolve(pathString);

			if(path.toFile().isDirectory())
				Files.walkFileTree(path, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
					{
						if(attrs.isRegularFile() && path.toString().endsWith(Katana.SOURCE_FILE_EXTENSION))
							paths.add(path);

						return FileVisitResult.CONTINUE;
					}
				});
			else
				paths.add(path);
		}

		return paths;
	}
}
