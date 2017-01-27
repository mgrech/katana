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

package katana.project;

import katana.Katana;
import katana.diag.CompileException;
import katana.project.conditionals.AlwaysTrue;
import katana.project.conditionals.Condition;
import katana.project.conditionals.ConditionParser;
import katana.project.conditionals.Conditional;
import katana.utils.JsonUtils;
import katana.utils.Maybe;
import katana.utils.ResourceExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
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

	private static boolean addFileToProject(Project project, Path path, Condition condition)
	{
		String pathString = path.toString();

		if(pathString.endsWith(Katana.FILE_EXTENSION_KATANA))
		{
			project.katanaFiles.add(new Conditional<>(condition, path));
			return true;
		}

		if(!pathString.endsWith(Katana.FILE_EXTENSION_ASM)
		&& !pathString.endsWith(Katana.FILE_EXTENSION_C)
		&& !pathString.endsWith(Katana.FILE_EXTENSION_CPP))
			return false;

		project.externFiles.add(new Conditional<>(condition, path));
		return true;
	}

	private static void discoverSourceFiles(Project project, Path path, Condition condition) throws IOException
	{
		File file = path.toFile();

		if(!file.exists())
			throw new CompileException(String.format("file or directory '%s' does not exit", path));

		if(path.toFile().isDirectory())
			Files.walkFileTree(path, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
				{
					if(attrs.isRegularFile())
						addFileToProject(project, path, condition);

					return FileVisitResult.CONTINUE;
				}
			});

		else if(!addFileToProject(project, path, condition))
			throw new CompileException(String.format("source file path '%s' refers to unknown file type", path));
	}

	private static void discoverSourceFiles(Project project, List<String> sourcePaths) throws IOException
	{
		for(String sourcePath : sourcePaths)
		{
			String[] components = sourcePath.split(":");
			Path path = project.root.resolve(components[components.length == 1 ? 0 : 1].trim());

			switch(components.length)
			{
			case 1:
				discoverSourceFiles(project, path, new AlwaysTrue());
				break;

			case 2:
				try
				{
					Condition condition = ConditionParser.parse(components[0]);
					discoverSourceFiles(project, path, condition);
				}

				catch(CompileException e)
				{
					String fmt = "error parsing source specification '%s': %s";
					throw new CompileException(String.format(fmt, sourcePath, e.getMessage()));
				}

				break;

			default: throw new AssertionError("unreachable");
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

	private static Project validateConfig(Path root, ProjectConfig config)
	{
		if(config.name == null)
			configErrorMissingProperty("name");

		validatePropertyValue("name", config.name, "[-A-Za-z0-9_]+");

		if(config.sources == null)
			configErrorMissingProperty("source");

		for(String sourcePath : config.sources)
		{
			String[] components = sourcePath.split(":");

			switch(components.length)
			{
			case 1:
				validatePath(root, components[0].trim());
				break;

			case 2:
				validatePath(root, components[1].trim());
				break;

			default:
				String fmt = "invalid source specification '%s', expected form '[condition:]path'";
				throw new CompileException(String.format(fmt, sourcePath));
			}
		}

		if(config.libs == null)
			configErrorMissingProperty("libs");

		List<Conditional<String>> libs = new ArrayList<>();

		for(String lib : config.libs)
		{
			String[] parts = lib.split(":");

			switch(parts.length)
			{
			case 1:
				parts[0] = parts[0].trim();
				validatePropertyValue("libs", parts[0], "[-A-Za-z0-9_]+");
				libs.add(new Conditional<>(new AlwaysTrue(), parts[0]));
				break;

			case 2:
				Condition condition = ConditionParser.parse(parts[0]);
				parts[1] = parts[1].trim();
				validatePropertyValue("libs", parts[1], "[-A-Za-z0-9_]+");
				libs.add(new Conditional<>(condition, parts[1]));
				break;

			default:
				String fmt = "invalid library dependency specification '%s', expected form '[condition:]lib'";
				throw new CompileException(String.format(fmt, lib));
			}
		}

		if(config.type == null)
			configErrorMissingProperty("type");

		if(config.katanaVersion == null)
			configErrorMissingProperty("katana-version");

		if(config.type == ProjectType.EXECUTABLE && config.entryPoint == null)
			configErrorMissingProperty("entry-point");

		if(config.type != ProjectType.EXECUTABLE && config.entryPoint != null)
			configError("property 'entry-point' is only applicable to executables");

		return new Project(root, config.name, config.type, libs, Maybe.wrap(config.entryPoint));
	}

	public static Project load(Path root) throws IOException
	{
		Path configPath = root.resolve(PROJECT_CONFIG_NAME);
		ProjectConfig config = JsonUtils.loadObject(configPath, ProjectConfig.class);

		Project project = validateConfig(root, config);
		discoverSourceFiles(project, config.sources);

		return project;
	}

	public static void createDefaultProject(Path path) throws IOException
	{
		ResourceExtractor.extract("project-template", path);
	}
}
