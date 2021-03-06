// Copyright 2017-2019 Markus Grech
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

package io.katana.compiler.backend.llvm;

import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.backend.llvm.codegen.StringPool;
import io.katana.compiler.project.BuildTarget;

public class FileCodegenContext
{
	private final BuildTarget build;
	private final PlatformContext platform;
	private final StringPool stringPool;

	public FileCodegenContext(BuildTarget build, PlatformContext platform, StringPool stringPool)
	{
		this.build = build;
		this.platform = platform;
		this.stringPool = stringPool;
	}

	public BuildTarget build()
	{
		return build;
	}

	public PlatformContext platform()
	{
		return platform;
	}

	public StringPool stringPool()
	{
		return stringPool;
	}
}
