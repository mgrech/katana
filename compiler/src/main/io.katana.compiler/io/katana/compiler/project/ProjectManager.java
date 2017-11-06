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

package io.katana.compiler.project;

import io.katana.compiler.Katana;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.project.conditionals.Condition;
import io.katana.compiler.project.conditionals.ConditionParser;
import io.katana.compiler.utils.FileUtils;
import io.katana.compiler.utils.JsonUtils;
import io.katana.compiler.utils.Maybe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

public class ProjectManager
{
	private static final Path PROJECT_TEMPLATE_PATH = Katana.HOME.resolve("template");
	private static final String PROJECT_CONFIG_NAME = "project.json";
	private static final Pattern LIB_NAME_PATTERN = Pattern.compile("[-A-Za-z0-9_]+");
	private static final Pattern PROJECT_NAME_PATTERN = Pattern.compile("[-A-Za-z0-9_]+");

	private static void configError(String fmt, Object... values)
	{
		throw new CompileException(String.format("%s: %s", PROJECT_CONFIG_NAME, String.format(fmt, values)));
	}

	private static void validateNonNull(String name, Object value)
	{
		if(value == null)
			configError("missing property '%s'", name);
	}

	private static void validatePropertyValue(String name, String value, Pattern pattern)
	{
		if(!pattern.matcher(value).matches())
			configError("property '%s' does not match pattern '%s', got '%s'", name, pattern, value);
	}

	// paths should be relative, normalized paths referring to files below the project root
	// symbolic links are disallowed to prevent breaking out of the project directory
	private static Path validatePath(Path root, String pathString) throws IOException
	{
		Path path = Paths.get(pathString);

		if(path.isAbsolute())
			throw new InvalidPathException(pathString, "given path is absolute");

		if(!path.normalize().equals(path))
			throw new InvalidPathException(pathString, "given path is not normalized");

		path = root.resolve(path).toRealPath();

		if(!path.startsWith(root))
			throw new InvalidPathException(path.toString(), "given path does not refer to a child of the project directory");

		return path;
	}

	private static FileType fileTypefromName(String name)
	{
		if(name.endsWith(".ks"))
			return FileType.KATANA;

		if(name.endsWith(".asm"))
			return FileType.ASM;

		if(name.endsWith(".c"))
			return FileType.C;

		if(name.endsWith(".cpp"))
			return FileType.CPP;

		return null;
	}

	private static void addFile(Map<FileType, Set<Path>> files, FileType type, Path path)
	{
		Set<Path> paths = files.computeIfAbsent(type, (t) -> new TreeSet<>());
		paths.add(path);
	}

	private static void discoverSourceFiles(Path path, Map<FileType, Set<Path>> files) throws IOException
	{
		File file = path.toFile();

		if(!file.exists())
			throw new CompileException(String.format("file or directory '%s' does not exit", path));

		if(file.isDirectory())
		{
			Files.walkFileTree(path, new SimpleFileVisitor<>()
			{
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
				{
					if(attrs.isRegularFile())
					{
						FileType type = fileTypefromName(path.getFileName().toString());

						if(type != null)
							addFile(files, type, path);
					}

					return FileVisitResult.CONTINUE;
				}
			});
		}
		else
		{
			FileType type = fileTypefromName(file.getName());

			if(type == null)
				throw new CompileException(String.format("source file path '%s' refers to unknown file type", path));

			addFile(files, type, path);
		}
	}

	private static Map<FileType, Set<Path>> validateSources(Path root, List<String> sources, TargetTriple target) throws IOException
	{
		List<Path> paths = new ArrayList<>();

		for(String sourcePath : sources)
		{
			String[] parts = sourcePath.split(":");

			for(int i = 0; i != parts.length; ++i)
				parts[i] = parts[i].trim();

			switch(parts.length)
			{
			case 1:
				paths.add(validatePath(root, parts[0]));
				break;

			case 2:
				Condition condition = ConditionParser.parse(parts[0]);
				Path path = validatePath(root, parts[1]);

				if(condition.test(target))
					paths.add(path);

				break;

			default:
				String fmt = "invalid source specification '%s', expected form '[condition:]path'";
				throw new CompileException(String.format(fmt, sourcePath));
			}
		}

		Map<FileType, Set<Path>> result = new HashMap<>();

		for(Path path : paths)
			discoverSourceFiles(path, result);

		return result;
	}

	private static List<String> validateLibs(List<String> libs, TargetTriple target)
	{
		List<String> result = new ArrayList<>();

		for(String lib : libs)
		{
			String[] parts = lib.split(":");

			for(int i = 0; i != parts.length; ++i)
				parts[i] = parts[i].trim();

			switch(parts.length)
			{
			case 1:
				validatePropertyValue("libs", parts[0], LIB_NAME_PATTERN);
				result.add(parts[0]);
				break;

			case 2:
				Condition condition = ConditionParser.parse(parts[0]);
				validatePropertyValue("libs", parts[1], LIB_NAME_PATTERN);

				if(condition.test(target))
					result.add(parts[1]);

				break;

			default:
				String fmt = "invalid library dependency specification '%s', expected form '[condition:]lib'";
				throw new CompileException(String.format(fmt, lib));
			}
		}

		return result;
	}

	private static Map<String, Path> loadResources(Path projectRoot) throws IOException
	{
		File resourceConfig = projectRoot.resolve("resources.txt").toFile();
		Properties properties = new Properties();

		if(resourceConfig.exists())
			properties.load(new FileInputStream(resourceConfig));

		Map<String, Path> resources = new TreeMap<>();

		for(Map.Entry<?, ?> entry : properties.entrySet())
		{
			String key = (String)entry.getKey();
			String path = (String)entry.getValue();
			resources.put(key, validatePath(projectRoot, path));
		}

		return resources;
	}

	private static Project validateConfig(Path root, ProjectConfig config, TargetTriple target) throws IOException
	{
		validateNonNull("name", config.name);
		validatePropertyValue("name", config.name, PROJECT_NAME_PATTERN);
		validateNonNull("sources", config.sources);
		validateNonNull("libs", config.libs);
		validateNonNull("type", config.type);
		validateNonNull("katana-version", config.katanaVersion);

		if(config.type == ProjectType.EXECUTABLE)
			validateNonNull("entry-point", config.entryPoint);

		if(config.type != ProjectType.EXECUTABLE && config.entryPoint != null)
			configError("property 'entry-point' is only applicable to executables");

		Map<FileType, Set<Path>> sources = validateSources(root, config.sources, target);
		List<String> libs = validateLibs(config.libs, target);
		Map<String, Path> resourceFiles = loadResources(root);
		return new Project(root, config.name, sources, libs, config.type, Maybe.wrap(config.entryPoint), resourceFiles);
	}

	public static Project load(Path root, TargetTriple target) throws IOException
	{
		Path configPath = root.resolve(PROJECT_CONFIG_NAME);
		ProjectConfig config = JsonUtils.loadObject(configPath, ProjectConfig.class);
		return validateConfig(root, config, target);
	}

	public static void createDefaultProject(Path path) throws IOException
	{
		FileUtils.copyDirectory(PROJECT_TEMPLATE_PATH, path);
	}
}