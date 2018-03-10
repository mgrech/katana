// Copyright 2017-2018 Markus Grech
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

import java.nio.file.Path;
import java.util.Map;

public class Project
{
	public Path root;
	public String name;
	public String version;
	public Map<String, BuildTarget> targets;
	public Path buildRoot;

	public Project(Path root, String name, String version, Map<String, BuildTarget> targets, Path buildRoot)
	{
		this.root = root;
		this.name = name;
		this.version = version;
		this.targets = targets;
		this.buildRoot = buildRoot;
	}
}
