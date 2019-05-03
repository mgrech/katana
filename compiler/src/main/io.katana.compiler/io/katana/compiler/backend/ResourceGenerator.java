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

package io.katana.compiler.backend;

import io.katana.compiler.diag.CompileException;
import io.katana.compiler.platform.TargetTriple;
import io.katana.compiler.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ResourceGenerator
{
	private final StringBuilder builder = new StringBuilder();
	private final TargetTriple target;
	private final Map<String, Path> resources;

	private ResourceGenerator(TargetTriple target, Map<String, Path> resources)
	{
		this.target = target;
		this.resources = resources;
	}

	private String intDirective()
	{
		switch((int)target.arch.pointerSize)
		{
		case 1: return "byte";
		case 2: return "word";
		case 4: return "int";
		case 8: return "quad";
		default: break;
		}

		throw new CompileException("unknown pointer size");
	}

	private void append(String format, Object... args)
	{
		builder.append(String.format(format, args));
	}

	private void generateInt(String format, Object... args)
	{
		append(".%s %s\n", intDirective(), String.format(format, args));
	}

	private String escape(String s)
	{
		var builder = new StringBuilder();

		s.chars().forEach(c -> {
			if(c == '"')
				builder.append("\\\"");
			else if(c == '\\')
				builder.append("\\\\");
			else if(c >= 0x20 && c < 0x7F)
				builder.appendCodePoint(c);
			else
				throw new CompileException("invalid codepoint in string to be emitted into asm file");
		});

		return '"' + builder.toString() + '"';
	}

	// PE, ELF: https://sourceware.org/binutils/docs/as/Section.html
	// Mach-O:  https://developer.apple.com/library/content/documentation/DeveloperTools/Reference/Assembler/040-Assembler_Directives/asm_directives.html
	// we want our section to be readable and writable
	private String resourceSectionDirective()
	{
		switch(target.os)
		{
		// on windows sections names are truncated to 8 bytes for executables, see
		// https://msdn.microsoft.com/en-us/library/windows/desktop/ms680547(v=vs.85).aspx#section_table__section_headers_
		case WINDOWS: return ".section .ktrsrcs, \"d\"\n";
		case LINUX:   return ".section .kt_resources, \"aw\"\n";
		case MACOS:   return ".section __KT, __resources\n";
		default: break;
		}

		throw new AssertionError("unknown os");
	}

	private void generateHeader()
	{
		append(resourceSectionDirective());
		append("\n");

		append(".globl __kt_resources\n");
		append("__kt_resources:\n");
		append(".align %s\n", target.arch.pointerAlign);
		generateInt("%s", resources.size());
		append("\n");
	}

	private void generateMetadata()
	{
		var i = 0;
		for(var entry : resources.entrySet())
		{
			generateInt("%s", entry.getKey().length());
			generateInt("key%s", i);
			generateInt("%s", entry.getValue().toFile().length());
			generateInt("res%s", i);

			++i;
		}

		append("\n");
	}

	private void generateData()
	{
		var i = 0;
		for(var key : resources.keySet())
			append("key%s: .ascii %s\n", i++, escape(key));

		var j = 0;
		for(var resourcePath : resources.values())
			append("res%s: .incbin %s\n", j++, escape(resourcePath.toString()));
	}

	private String generate()
	{
		generateHeader();
		generateMetadata();
		generateData();
		return builder.toString();
	}

	public static void generate(TargetTriple target, Map<String, Path> resources, Path outputPath) throws IOException
	{
		var generator = new ResourceGenerator(target, resources);
		FileUtils.writeFile(generator.generate(), outputPath);
	}
}
