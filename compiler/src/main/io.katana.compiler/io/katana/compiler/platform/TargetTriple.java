// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.platform;

public class TargetTriple
{
	public final Arch arch;
	public final Vendor vendor;
	public final Os os;
	public final Environment env;

	public TargetTriple(Arch arch, Vendor vendor, Os os, Environment env)
	{
		this.arch = arch;
		this.vendor = vendor;
		this.os = os;
		this.env = env;
	}

	@Override
	public String toString()
	{
		if(env == Environment.UNKNOWN)
			return String.format("%s-%s-%s", arch, vendor, os);

		return String.format("%s-%s-%s-%s", arch, vendor, os, env);
	}

	public static final TargetTriple NATIVE = detectNativeTarget();

	private static TargetTriple detectNativeTarget()
	{
		Arch arch = Arch.UNKNOWN;
		Vendor vendor = Vendor.UNKNOWN;
		Os os = Os.UNKNOWN;
		Environment env = Environment.UNKNOWN;

		String osArch = System.getProperty("os.arch").toLowerCase().replaceAll("[-_\\s]", "");
		String osName = System.getProperty("os.name").toLowerCase().replaceAll("[-_\\s]", "");

		if(osArch.equals("x86"))
			arch = Arch.X86;
		else if(osArch.equals("amd64") || osArch.equals("x8664"))
			arch = Arch.AMD64;

		if(osName.startsWith("windows"))
		{
			vendor = Vendor.PC;
			os = Os.WINDOWS;
			env = Environment.MSVC;
		}

		else if(osName.startsWith("mac"))
		{
			vendor = Vendor.APPLE;
			os = Os.MACOS;
		}

		else if(osName.startsWith("linux"))
		{
			os = Os.LINUX;
			env = Environment.GNU;
		}

		return new TargetTriple(arch, vendor, os, env);
	}
}
