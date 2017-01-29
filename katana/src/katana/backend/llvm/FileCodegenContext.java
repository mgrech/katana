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

package katana.backend.llvm;

import katana.backend.PlatformContext;
import katana.project.Project;

public class FileCodegenContext
{
	private final Project project;
	private final PlatformContext platform;
	private final StringBuilder builder;
	private final StringPool stringPool;

	public FileCodegenContext(Project project, PlatformContext platform, StringBuilder builder, StringPool stringPool)
	{
		this.project = project;
		this.platform = platform;
		this.builder = builder;
		this.stringPool = stringPool;
	}

	public Project project()
	{
		return project;
	}

	public PlatformContext platform()
	{
		return platform;
	}

	public StringPool stringPool()
	{
		return stringPool;
	}

	public <T> void write(T value)
	{
		builder.append(value);
	}

	public void writef(String fmt, Object... values)
	{
		builder.append(String.format(fmt, values));
	}
}
