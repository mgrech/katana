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

import io.katana.compiler.platform.TargetTriple;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VsTools
{
	private static final Path VSTOOLS_PATH = Paths.get(System.getenv("KATANA_VSTOOLS_HOME")).toAbsolutePath().normalize();
	private static final Path DEVCMD_PATH = VSTOOLS_PATH.resolve("Common7/Tools/VsDevCmd.bat");

	private static String archToDevCmdArch(TargetTriple target)
	{
		switch(target.arch)
		{
		case AMD64: return "amd64";
		case X86: return "x86";
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	public static ProcessBuilder buildDevCmdCommand(List<String> command, TargetTriple target)
	{
		List<String> fullCommand = new ArrayList<>();
		fullCommand.add("cmd");
		fullCommand.add("/S");
		fullCommand.add("/C");

		// parameter description in Common7\Tools\vsdevcmd\core\parse_cmd.bat
		String cmdFmt = "\"\"%s\" -no_logo -arch=%s -host_arch=%s >nul 2>&1 && %s\"";
		fullCommand.add(String.format(cmdFmt, DEVCMD_PATH, archToDevCmdArch(target), archToDevCmdArch(TargetTriple.NATIVE), String.join(" ", command)));

		return new ProcessBuilder(fullCommand);
	}
}
