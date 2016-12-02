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

package katana.project;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import katana.Katana;
import katana.diag.CompileException;
import katana.utils.Maybe;
import katana.utils.ResourceExtractor;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class ProjectManager
{
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
	                                                  .serializeSpecialFloatingPointValues()
	                                                  .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
	                                                  .setLenient()
	                                                  .setPrettyPrinting()
	                                                  .create();

	private static final String PROJECT_CONFIG_NAME = "project.json";

	private static void validatePath(String pathString, Path root)
	{
		Path path = Paths.get(pathString);

		if(path.isAbsolute())
			throw new InvalidPathException(pathString, "given source path is absolute");

		if(!root.resolve(path).toAbsolutePath().normalize().startsWith(root))
			throw new InvalidPathException(path.toString(), "given source path is not a child of the project root");
	}

	private static ProjectConfig loadConfig(Path root) throws IOException
	{
		byte[] bytes = Files.readAllBytes(root.resolve(PROJECT_CONFIG_NAME));
		ProjectConfig project = GSON.fromJson(new StringReader(new String(bytes, StandardCharsets.UTF_8)), ProjectConfig.class);

		if(project == null)
			throw new JsonSyntaxException(String.format("%s is empty", PROJECT_CONFIG_NAME));

		for(String sourcePath : project.sourcePaths)
			validatePath(sourcePath, root);

		return project;
	}

	private static void discoverSourceFiles(Path root, ProjectConfig config, Project project) throws IOException
	{
		for(String sourcePath : config.sourcePaths)
		{
			Path path = root.resolve(sourcePath);

			if(path.toFile().isDirectory())
				Files.walkFileTree(path, new SimpleFileVisitor<Path>()
				{
					@Override
					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
					{
						if(attrs.isRegularFile())
						{
							String pathString = path.toString();

							if(pathString.endsWith(Katana.KATANA_SOURCE_FILE_EXTENSION))
								project.katanaFiles.add(path);
							else if(pathString.endsWith(Katana.C_SOURCE_FILE_EXTENSION))
								project.cFiles.add(path);
						}

						return FileVisitResult.CONTINUE;
					}
				});

			else
			{
				String pathString = path.toString();

				if(pathString.endsWith(Katana.KATANA_SOURCE_FILE_EXTENSION))
					project.katanaFiles.add(path);
				else if(pathString.endsWith(Katana.C_SOURCE_FILE_EXTENSION))
					project.cFiles.add(path);
				else
					throw new CompileException(String.format("source file path '%s' refers to unknown file type", pathString));
			}
		}
	}

	public static Project load(Path root) throws IOException
	{
		ProjectConfig config = loadConfig(root);
		Project project = new Project(root, Maybe.wrap(config.entryPoint));
		discoverSourceFiles(root, config, project);
		return project;
	}

	public static void createDefaultProject(Path path) throws IOException
	{
		ResourceExtractor.extract("project-template", path);
	}
}
