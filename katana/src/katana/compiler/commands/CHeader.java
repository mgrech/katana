// Copyright 2016 Markus Grech
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

package katana.compiler.commands;

import katana.compiler.Command;
import katana.utils.JarUtils;

import java.nio.charset.StandardCharsets;

@Command(name = "cheader", desc = "prints out C header file for use with katana")
public class CHeader
{
	public static void run(String[] args)
	{
		byte[] header = JarUtils.read("katana.h");
		System.out.println(new String(header, StandardCharsets.UTF_8));
	}
}
