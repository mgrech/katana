// Copyright 2016-2019 Markus Grech
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
import io.katana.compiler.project.conditionals.ConditionParser;
import io.katana.compiler.project.toml.ProfileToml;
import io.katana.compiler.project.toml.ProjectToml;
import io.katana.compiler.project.toml.TargetToml;
import io.katana.compiler.utils.FileUtils;
import io.katana.compiler.utils.TomlUtils;
import io.ous.jtoml.TomlTable;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
		var path = Paths.get(pathString);

		if(path.isAbsolute())
			configError("path is absolute: %s", pathString);

		if(!path.normalize().equals(path))
			configError("path is not normalized: %s", pathString);

		path = root.resolve(path).toRealPath();

		if(!path.startsWith(root))
			configError("path does not refer to a child of the project directory: %s", pathString);

		return root.relativize(path);
	}

	private static void addFile(Map<FileType, Set<Path>> files, FileType type, Path path)
	{
		var paths = files.computeIfAbsent(type, (t) -> new TreeSet<>());
		paths.add(path);
	}

	private static void discoverSourceFiles(Path path, Map<FileType, Set<Path>> files) throws IOException
	{
		var file = path.toFile();

		if(!file.exists())
			configError("file or directory '%s' does not exit", path);

		if(file.isDirectory())
		{
			Files.walkFileTree(path, new SimpleFileVisitor<>()
			{
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
				{
					if(attrs.isRegularFile())
					{
						var type = FileType.of(path.getFileName().toString());

						if(type.isSome())
							addFile(files, type.unwrap(), path);
					}

					return FileVisitResult.CONTINUE;
				}
			});
		}
		else
		{
			var type = FileType.of(file.getName());

			if(type.isNone())
				configError("source file path '%s' refers to unknown file type", path);

			addFile(files, type.unwrap(), path);
		}
	}

	private static Map<FileType, Set<Path>> validateSources(Path root, List<String> sources, TargetTriple target) throws IOException
	{
		var paths = new ArrayList<Path>();

		for(var sourcePath : sources)
		{
			var parts = sourcePath.split(":");

			for(var i = 0; i != parts.length; ++i)
				parts[i] = parts[i].trim();

			switch(parts.length)
			{
			case 1:
				paths.add(validatePath(root, parts[0]));
				break;

			case 2:
				var condition = ConditionParser.parse(parts[0]);
				var path = validatePath(root, parts[1]);

				if(condition.test(target))
					paths.add(path);

				break;

			default:
				configError("invalid source specification '%s', expected form '[condition:]path'", sourcePath);
			}
		}

		var result = new HashMap<FileType, Set<Path>>();

		for(var path : paths)
			discoverSourceFiles(path, result);

		return result;
	}

	private static List<String> validateLibs(List<String> libs, TargetTriple target)
	{
		var result = new ArrayList<String>();

		for(var lib : libs)
		{
			var parts = lib.split(":");

			for(var i = 0; i != parts.length; ++i)
				parts[i] = parts[i].trim();

			switch(parts.length)
			{
			case 1:
				validatePropertyValue("system-libraries", parts[0], LIB_NAME_PATTERN);
				result.add(parts[0]);
				break;

			case 2:
				var condition = ConditionParser.parse(parts[0]);
				validatePropertyValue("system-libraries", parts[1], LIB_NAME_PATTERN);

				if(condition.test(target))
					result.add(parts[1]);

				break;

			default:
				configError("invalid library dependency specification '%s', expected form '[condition:]lib'", lib);
			}
		}

		return result;
	}

	private static Map<String, Path> loadResourceList(Path projectRoot, Path path) throws IOException
	{
		var resourceConfig = projectRoot.resolve(path).toFile();
		var properties = new Properties();

		if(resourceConfig.exists())
			properties.load(new FileInputStream(resourceConfig));

		var resources = new TreeMap<String, Path>();

		for(var entry : properties.entrySet())
		{
			var key = (String)entry.getKey();
			var resourcePath = (String)entry.getValue();
			resources.put(key, validatePath(projectRoot, projectRoot.relativize(path.getParent().resolve(resourcePath)).toString()));
		}

		return resources;
	}

	private static List<String> validateOptions(List<String> options, TargetTriple target)
	{
		var result = new ArrayList<String>();

		for(String option : options)
		{
			var parts = option.split(":");

			switch(parts.length)
			{
			case 1:
				result.add(option);
				break;

			case 2:
				var condition = ConditionParser.parse(parts[0]);

				if(condition.test(target))
					result.add(parts[1]);

				break;

			default:
				configError("invalid option specification '%s', expected form [condition:]option", option);
			}
		}

		return result;
	}

	private static BuildTarget validateTarget(Path root, String name, TargetToml toml, TargetTriple target) throws IOException
	{
		validateNonNull("type", toml.type);
		validateNonNull("sources", toml.sources);

		BuildType type = null;

		try
		{
			type = BuildType.valueOf(toml.type.toUpperCase().replaceAll("-", "_"));
		}
		catch(IllegalArgumentException ex)
		{
			configError("invalid target type '%s'", toml.type);
		}

		// entry-point may be null for executables as well to allow entry points in other languages

		if(type != BuildType.EXECUTABLE && toml.entryPoint != null)
			configError("property 'entry-point' is only applicable to executables");

		var result = new BuildTarget();
		result.name = name;
		result.type = type;
		result.entryPoint = toml.entryPoint;
		result.sourceFiles = validateSources(root, toml.sources, target);
		result.asmOptions = validateOptions(toml.asmOptions, target);
		result.cOptions = validateOptions(toml.cOptions, target);
		result.cppOptions = validateOptions(toml.cppOptions, target);
		result.llvmOptions = validateOptions(toml.llvmOptions, target);
		result.linkOptions = validateOptions(toml.linkOptions, target);
		result.dependencies = new ArrayList<>();
		result.systemLibraries = validateLibs(toml.systemLibraries, target);

		if(toml.resourceList != null && !toml.resourceList.isEmpty())
			result.resourceFiles = loadResourceList(root, validatePath(root, toml.resourceList));
		else
			result.resourceFiles = new TreeMap<>();

		return result;
	}

	private static List<String> mergeUnique(List<String> first, List<String> second)
	{
		var result = new ArrayList<>(first);

		for(var s : second)
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

		for(var inheritedName : profile.inherit)
		{
			var inherited = profiles.get(inheritedName);
			flattenProfileHierarchy(inherited, profiles, flattened);
			copyOptions(inherited, profile);
		}
	}

	private static void flattenProfileHierarchy(Map<String, ProfileToml> profiles)
	{
		var flattened = Collections.newSetFromMap(new IdentityHashMap<ProfileToml, Boolean>());

		for(var profile : profiles.values())
			flattenProfileHierarchy(profile, profiles, flattened);
	}

	private static void applyProfiles(Map<String, BuildTarget> buildTargets,
	                                  Map<String, TargetToml> targetTomls,
	                                  Map<String, ProfileToml> profileTomls,
	                                  Set<String> buildProfiles,
	                                  TargetTriple target)
	{
		for(var build : buildTargets.values())
		{
			var targetToml = targetTomls.get(build.name);

			for(var buildProfile : buildProfiles)
				if(!targetToml.profiles.contains(buildProfile))
					targetToml.profiles.add(buildProfile);

			for(var profileName : targetToml.profiles)
			{
				var profileToml = profileTomls.get(profileName);

				if(profileToml == null)
					configError("unknown profile '%s'", profileName);

				build.asmOptions .addAll(validateOptions(profileToml.asmOptions,  target));
				build.cOptions   .addAll(validateOptions(profileToml.cOptions,    target));
				build.cppOptions .addAll(validateOptions(profileToml.cppOptions,  target));
				build.llvmOptions.addAll(validateOptions(profileToml.llvmOptions, target));
				build.linkOptions.addAll(validateOptions(profileToml.linkOptions, target));
			}
		}
	}

	private static void walkDependencyGraph(BuildTarget target, List<BuildTarget> seen)
	{
		if(seen.contains(target))
		{
			while(seen.get(0) != target)
				seen.remove(0);

			seen.add(target);
			var cycle = seen.stream().map(t -> t.name).collect(Collectors.joining(" -> "));
			configError("cyclic dependency detected: %s", cycle);
		}

		seen.add(target);

		for(var dependency : target.dependencies)
			walkDependencyGraph(dependency, seen);
	}

	private static void ensureDependencyCycleFree(Collection<BuildTarget> targets)
	{
		for(var target : targets)
		{
			var seen = new ArrayList<BuildTarget>();
			walkDependencyGraph(target, seen);
		}
	}

	private static void validateDependencies(Map<String, BuildTarget> targets, Map<String, TargetToml> targetTomls)
	{
		for(var targetEntry : targets.entrySet())
		{
			var name = targetEntry.getKey();
			var target = targetEntry.getValue();
			var toml = targetTomls.get(name);

			for(var dependencyName : toml.dependencies)
			{
				var dependency = targets.get(dependencyName);

				if(dependency == null)
					configError("target '%s': unknown dependency '%s'", name, dependencyName);

				if(dependency.name.equals(name))
					configError("target '%s': target cannot depend on itself", name);

				if(dependency.type != BuildType.LIBRARY_STATIC && dependency.type != BuildType.LIBRARY_SHARED)
					configError("target '%s': dependency '%s' is not a library", name, dependencyName);

				target.dependencies.add(dependency);
			}
		}

		ensureDependencyCycleFree(targets.values());
	}

	private static Project validateConfig(Path root, Set<String> buildProfiles, ProjectToml toml, TargetTriple target) throws IOException
	{
		validateNonNull("katana-version", toml.katanaVersion);
		validateNonNull("name", toml.name);
		validatePropertyValue("name", toml.name, PROJECT_NAME_PATTERN);
		validateNonNull("version", toml.version);
		validatePropertyValue("version", toml.version, PROJECT_VERSION_PATTERN);

		var targets = new HashMap<String, BuildTarget>();

		for(var entry : toml.targets.entrySet())
		{
			String name = entry.getKey();
			targets.put(name, validateTarget(root, name, entry.getValue(), target));
		}

		flattenProfileHierarchy(toml.profiles);
		applyProfiles(targets, toml.targets, toml.profiles, buildProfiles, target);
		validateDependencies(targets, toml.targets);

		return new Project(toml.name, toml.version, targets);
	}

	private static Map<String, ProfileToml> loadDefaultProfiles() throws IOException
	{
		var toml = TomlUtils.loadToml(DEFAULT_PROFILE_PATH);
		var profiles = new HashMap<String, ProfileToml>();

		for(var entry : toml.entrySet())
			profiles.put(entry.getKey(), ((TomlTable)entry.getValue()).asObject(ProfileToml.class));

		return profiles;
	}

	private static ProjectToml loadConfig(Path path) throws IOException
	{
		var toml = TomlUtils.loadToml(path);
		var config = toml.asObject(ProjectToml.class);

		var targetsToml = toml.getTomlTable("targets");

		if(targetsToml != null)
		{
			for(var entry : targetsToml.toMap().entrySet())
			{
				config.targets.put(entry.getKey(), ((TomlTable)entry.getValue()).asObject(TargetToml.class));
			}
		}

		var profilesToml = toml.getTomlTable("profiles");

		if(profilesToml != null)
		{
			for(var entry : profilesToml.toMap().entrySet())
			{
				config.profiles.put(entry.getKey(), ((TomlTable)entry.getValue()).asObject(ProfileToml.class));
			}
		}

		return config;
	}

	public static Project load(Path root, Set<String> buildProfiles, TargetTriple target) throws IOException
	{
		var config = loadConfig(root.resolve(PROJECT_CONFIG_NAME));
		config.profiles.putAll(loadDefaultProfiles());
		return validateConfig(root, buildProfiles, config, target);
	}

	public static void createDefaultProject(Path path) throws IOException
	{
		FileUtils.copyDirectory(PROJECT_TEMPLATE_PATH, path);
	}

	public static Path locateProjectRoot() throws IOException
	{
		var current = Paths.get("").toRealPath();

		for(; current != null; current = current.getParent())
			if(current.resolve(PROJECT_CONFIG_NAME).toFile().exists())
				return current;

		return null;
	}
}
