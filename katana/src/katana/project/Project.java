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

package katana.project;

import katana.project.conditionals.Conditional;
import katana.utils.Maybe;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Project
{
	public final Path root;
	public final String name;
	public final List<Conditional<String>> libs;
	public final ProjectType type;
	public final Maybe<String> entryPoint;

	public final List<Conditional<Path>> katanaFiles = new ArrayList<>();
	public final List<Conditional<Path>> externFiles = new ArrayList<>();

	public Project(Path root, String name, ProjectType type, List<Conditional<String>> libs, Maybe<String> entryPoint)
	{
		this.name = name;
		this.root = root;
		this.type = type;
		this.libs = libs;
		this.entryPoint = entryPoint;
	}
}
