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
import katana.platform.TargetTriple;
import katana.project.Project;
import katana.project.conditionals.Conditional;
import katana.scanner.Scanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProgramParser
{
	public static AstProgram parse(Project project, TargetTriple target) throws IOException
	{
		AstProgram program = new AstProgram();

		for(Conditional<Path> cpath : project.katanaFiles)
		{
			Path path = cpath.get(target);

			if(path == null)
				continue;

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
