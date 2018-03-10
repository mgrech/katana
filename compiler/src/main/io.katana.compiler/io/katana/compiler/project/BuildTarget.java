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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuildTarget
{
	public String name;
	public BuildType type;
	public String entryPoint;
	public Map<FileType, Set<Path>> sourceFiles;
	public List<String> asmOptions;
	public List<String> cOptions;
	public List<String> cppOptions;
	public List<String> llvmOptions;
	public List<String> linkOptions;
	public List<String> systemLibraries;
	public Map<String, Path> resourceFiles;
	public Path outputDirectory;
}
