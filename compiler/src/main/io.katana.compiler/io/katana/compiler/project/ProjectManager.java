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
import io.katana.compiler.project.toml.ProfileToml;
import io.katana.compiler.project.toml.ProjectToml;
import io.katana.compiler.project.toml.TargetToml;
import io.katana.compiler.utils.FileUtils;
import io.katana.compiler.utils.TomlUtils;
import io.ous.jtoml.TomlTable;

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
	private static final Path DEFAULT_PROFILE_PATH = Katana.HOME.resolve("profiles.toml");
	private static final String PROJECT_CONFIG_NAME = "project.toml";

	private static final Pattern LIB_NAME_PATTERN = Pattern.compile("[-A-Za-z0-9_]+");
	private static final Pattern PROJECT_NAME_PATTERN = Pattern.compile("[-A-Za-z0-9_]+");
	private static final Pattern PROJECT_VERSION_PATTERN = Pattern.compile("(0|(?:[1-9][0-9]*))\\.(0|(?:[1-9][0-9]*))(\\.(0|(?:[1-9][0-9]*)))?");

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
				validatePropertyValue("system-libraries", parts[0], LIB_NAME_PATTERN);
				result.add(parts[0]);
				break;

			case 2:
				Condition condition = ConditionParser.parse(parts[0]);
				validatePropertyValue("system-libraries", parts[1], LIB_NAME_PATTERN);

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

	private static Map<String, Path> loadResourceList(Path projectRoot, Path path) throws IOException
	{
		File resourceConfig = projectRoot.resolve(path).toFile();
		Properties properties = new Properties();

		if(resourceConfig.exists())
			properties.load(new FileInputStream(resourceConfig));

		Map<String, Path> resources = new TreeMap<>();

		for(Map.Entry<?, ?> entry : properties.entrySet())
		{
			String key = (String)entry.getKey();
			String resourcePath = (String)entry.getValue();
			resources.put(key, validatePath(projectRoot, projectRoot.relativize(path.getParent().resolve(resourcePath)).toString()));
		}

		return resources;
	}

	private static List<String> validateOptions(List<String> options, TargetTriple target)
	{
		List<String> result = new ArrayList<>();

		for(String option : options)
		{
			String[] parts = option.split(":");

			switch(parts.length)
			{
			case 1:
				result.add(option);
				break;

			case 2:
				Condition condition = ConditionParser.parse(parts[0]);

				if(condition.test(target))
					result.add(parts[1]);

				break;

			default:
				throw new CompileException(String.format("invalid option specification '%s', expected form [condition:]option", option));
			}
		}

		return result;
	}

	private static BuildTarget validateTarget(Path root, Path buildRoot, String name, TargetToml toml, TargetTriple target) throws IOException
	{
		validateNonNull("type", toml.type);
		validateNonNull("sources", toml.sources);

		BuildType type;

		try
		{
			type = BuildType.valueOf(toml.type.toUpperCase());
		}
		catch(IllegalArgumentException ex)
		{
			throw new CompileException(String.format("invalid target type '%s'", toml.type));
		}

		if(type == BuildType.EXECUTABLE)
			validateNonNull("entry-point", toml.entryPoint);

		if(type != BuildType.EXECUTABLE && toml.entryPoint != null)
			configError("property 'entry-point' is only applicable to executables");

		BuildTarget result = new BuildTarget();
		result.name = name;
		result.type = type;
		result.entryPoint = toml.entryPoint;
		result.sourceFiles = validateSources(root, toml.sources, target);
		result.asmOptions = validateOptions(toml.asmOptions, target);
		result.cOptions = validateOptions(toml.cOptions, target);
		result.cppOptions = validateOptions(toml.cppOptions, target);
		result.llvmOptions = validateOptions(toml.llvmOptions, target);
		result.linkOptions = validateOptions(toml.linkOptions, target);
		result.systemLibraries = validateLibs(toml.systemLibraries, target);

		if(toml.resourceList != null && !toml.resourceList.isEmpty())
			result.resourceFiles = loadResourceList(root, validatePath(root, toml.resourceList));
		else
			result.resourceFiles = new TreeMap<>();

		result.outputDirectory = buildRoot.resolve(name);

		return result;
	}

	private static List<String> mergeUnique(List<String> first, List<String> second)
	{
		List<String> result = new ArrayList<>();
		result.addAll(first);

		for(String s : second)
			if(!result.contains(s))
				result.add(s);

		return result;
	}

	private static void copyOptions(ProfileToml src, ProfileToml dst)
	{
		dst.asmOptions  = mergeUnique(src.asmOptions,  dst.asmOptions);
		dst.cOptions    = mergeUnique(src.cOptions,    dst.cOptions);
		dst.cppOptions  = mergeUnique(src.cppOptions,  dst.cppOptions);
		dst.llvmOptions = mergeUnique(src.llvmOptions, dst.llvmOptions);
		dst.linkOptions = mergeUnique(src.linkOptions, dst.linkOptions);
	}

	private static void flattenProfileHierarchy(ProfileToml profile, Map<String, ProfileToml> profiles, Set<ProfileToml> flattened)
	{
		if(flattened.contains(profile))
			return;

		flattened.add(profile);

		for(String inheritedName : profile.inherit)
		{
			ProfileToml inherited = profiles.get(inheritedName);
			flattenProfileHierarchy(inherited, profiles, flattened);
			copyOptions(inherited, profile);
		}
	}

	private static void flattenProfileHierarchy(Map<String, ProfileToml> profiles)
	{
		Set<ProfileToml> flattened = Collections.newSetFromMap(new IdentityHashMap<>());

		for(ProfileToml profile : profiles.values())
			flattenProfileHierarchy(profile, profiles, flattened);
	}

	private static void applyProfiles(Map<String, BuildTarget> targets,
	                                  Map<String, TargetToml> targetTomls,
	                                  Map<String, ProfileToml> profileTomls,
	                                  Set<String> buildProfiles)
	{
		for(BuildTarget target : targets.values())
		{
			TargetToml targetToml = targetTomls.get(target.name);

			for(String buildProfile : buildProfiles)
				if(!targetToml.profiles.contains(buildProfile))
					targetToml.profiles.add(buildProfile);

			for(String profileName : targetToml.profiles)
			{
				ProfileToml profileToml = profileTomls.get(profileName);

				if(profileToml == null)
					throw new CompileException(String.format("unknown profile '%s'", profileName));

				target.asmOptions.addAll(profileToml.asmOptions);
				target.cOptions.addAll(profileToml.cOptions);
				target.cppOptions.addAll(profileToml.cppOptions);
				target.llvmOptions.addAll(profileToml.llvmOptions);
				target.linkOptions.addAll(profileToml.linkOptions);
			}
		}
	}

	private static Project validateConfig(Path root, Path buildRoot, Set<String> buildProfiles, ProjectToml toml, TargetTriple target) throws IOException
	{
		validateNonNull("katana-version", toml.katanaVersion);
		validateNonNull("name", toml.name);
		validatePropertyValue("name", toml.name, PROJECT_NAME_PATTERN);
		validateNonNull("version", toml.version);
		validatePropertyValue("version", toml.version, PROJECT_VERSION_PATTERN);

		Map<String, BuildTarget> targets = new HashMap<>();

		for(Map.Entry<String, TargetToml> entry : toml.targets.entrySet())
		{
			String name = entry.getKey();
			targets.put(name, validateTarget(root, buildRoot, name, entry.getValue(), target));
		}

		flattenProfileHierarchy(toml.profiles);
		applyProfiles(targets, toml.targets, toml.profiles, buildProfiles);

		return new Project(root, toml.name, toml.version, targets, buildRoot);
	}

	private static Map<String, ProfileToml> loadDefaultProfiles() throws IOException
	{
		TomlTable toml = TomlUtils.loadToml(DEFAULT_PROFILE_PATH);
		Map<String, ProfileToml> profiles = new HashMap<>();

		for(Map.Entry<String, Object> entry : toml.entrySet())
			profiles.put(entry.getKey(), ((TomlTable)entry.getValue()).asObject(ProfileToml.class));

		return profiles;
	}

	private static ProjectToml loadConfig(Path path) throws IOException
	{
		TomlTable toml = TomlUtils.loadToml(path);
		ProjectToml config = toml.asObject(ProjectToml.class);

		TomlTable targetsToml = toml.getTomlTable("targets");

		if(targetsToml != null)
		{
			for(Map.Entry<String, Object> entry : targetsToml.toMap().entrySet())
			{
				config.targets.put(entry.getKey(), ((TomlTable)entry.getValue()).asObject(TargetToml.class));
			}
		}

		TomlTable profilesToml = toml.getTomlTable("profiles");

		if(profilesToml != null)
		{
			for(Map.Entry<String, Object> entry : profilesToml.toMap().entrySet())
			{
				config.profiles.put(entry.getKey(), ((TomlTable)entry.getValue()).asObject(ProfileToml.class));
			}
		}

		return config;
	}

	public static Project load(Path root, Path buildRoot, Set<String> buildProfiles, TargetTriple target) throws IOException
	{
		ProjectToml config = loadConfig(root.resolve(PROJECT_CONFIG_NAME));
		config.profiles.putAll(loadDefaultProfiles());
		return validateConfig(root, buildRoot, buildProfiles, config, target);
	}

	public static void createDefaultProject(Path path) throws IOException
	{
		FileUtils.copyDirectory(PROJECT_TEMPLATE_PATH, path);
	}

	public static Path locateProjectRoot() throws IOException
	{
		Path current = Paths.get("").toRealPath();

		for(; current != null; current = current.getParent())
			if(current.resolve(PROJECT_CONFIG_NAME).toFile().exists())
				return current;

		return null;
	}
}
