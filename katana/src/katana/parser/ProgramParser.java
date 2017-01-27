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

package katana.parser;

import katana.ast.AstFile;
import katana.ast.AstProgram;
import katana.project.FileType;
import katana.project.Project;
import katana.scanner.Scanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class ProgramParser
{
	public static AstProgram parse(Project project) throws IOException
	{
		Set<Path> katanaFiles = project.sourceFiles.get(FileType.KATANA);

		if(katanaFiles == null)
			return null;

		AstProgram program = new AstProgram();

		for(Path path : katanaFiles)
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
}
