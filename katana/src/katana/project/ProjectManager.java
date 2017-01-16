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

import katana.Katana;
import katana.diag.CompileException;
import katana.utils.JsonUtils;
import katana.utils.Maybe;
import katana.utils.ResourceExtractor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class ProjectManager
{
	private static final String PROJECT_CONFIG_NAME = "project.json";

	private static void validatePath(Path root, String pathString)
	{
		Path path = Paths.get(pathString);

		if(path.isAbsolute())
			throw new InvalidPathException(pathString, "given source path is absolute");

		if(!root.resolve(path).toAbsolutePath().normalize().startsWith(root))
			throw new InvalidPathException(path.toString(), "given source path is not a child of the project root");
	}

	private static void discoverSourceFiles(Project project, List<String> sourcePaths) throws IOException
	{
		for(String sourcePath : sourcePaths)
		{
			Path path = project.root.resolve(sourcePath);

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
							else if(pathString.endsWith(Katana.CPP_SOURCE_FILE_EXTENSION))
								project.cppFiles.add(path);
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
				else if(path.endsWith(Katana.CPP_SOURCE_FILE_EXTENSION))
					project.cppFiles.add(path);
				else
					throw new CompileException(String.format("source file path '%s' refers to unknown file type", pathString));
			}
		}
	}

	private static void configError(String fmt, Object... values)
	{
		throw new CompileException(String.format("%s: %s", PROJECT_CONFIG_NAME, String.format(fmt, values)));
	}

	private static void configErrorMissingProperty(String name)
	{
		configError("missing property '%s'", name);
	}

	private static void validatePropertyValue(String name, String value, String pattern)
	{
		if(!value.matches(pattern))
			configError("property '%s' does not match pattern '%s', got '%s'", name, pattern, value);
	}

	private static void validateConfig(Path root, ProjectConfig config)
	{
		if(config.name == null)
			configErrorMissingProperty("name");

		validatePropertyValue("name", config.name, "[-A-Za-z0-9]+");

		if(config.sourcePaths == null)
			configErrorMissingProperty("source-paths");

		for(String sourcePath : config.sourcePaths)
			validatePath(root, sourcePath);

		if(config.type == null)
			configErrorMissingProperty("type");

		if(config.katanaVersion == null)
			configErrorMissingProperty("katana-version");

		if(config.type == ProjectType.EXECUTABLE && config.entryPoint == null)
			configErrorMissingProperty("entry-point");

		if(config.type != ProjectType.EXECUTABLE && config.entryPoint != null)
			configError("property 'entry-point' is only applicable to executables");
	}

	public static Project load(Path root) throws IOException
	{
		Path configPath = root.resolve(PROJECT_CONFIG_NAME);
		ProjectConfig config = JsonUtils.loadObject(configPath, ProjectConfig.class);
		validateConfig(root, config);

		Project project = new Project(root, config.name, config.type, Maybe.wrap(config.entryPoint));
		discoverSourceFiles(project, config.sourcePaths);

		return project;
	}

	public static void createDefaultProject(Path path) throws IOException
	{
		ResourceExtractor.extract("project-template", path);
	}
}
