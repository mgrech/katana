// Copyright 2017 Markus Grech
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

import io.katana.compiler.utils.Maybe;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Project
{
	public final Path root;
	public final String name;
	public final Map<FileType, Set<Path>> sourceFiles;
	public final List<String> libraries;
	public final ProjectType type;
	public final Maybe<String> entryPoint;
	public final Map<String, Path> resourceFiles;

	public Project(Path root, String name, Map<FileType, Set<Path>> sourceFiles, List<String> libraries, ProjectType type, Maybe<String> entryPoint, Map<String, Path> resourceFiles)
	{
		this.root = root;
		this.name = name;
		this.sourceFiles = sourceFiles;
		this.libraries = libraries;
		this.type = type;
		this.entryPoint = entryPoint;
		this.resourceFiles = resourceFiles;
	}
}
