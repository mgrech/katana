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
		switch(target.arch.pointerSize.intValue())
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
		append("\t.%s %s\n", intDirective(), String.format(format, args));
	}

	private String escape(String s)
	{
		StringBuilder builder = new StringBuilder();

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

	private void generateHeader()
	{
		append(".section .kt_resources, \"ad\"\n");
		append(".globl __kt_resources\n");
		append("__kt_resources:\n");
		append("\t.align %s\n", target.arch.pointerAlign);
		generateInt("(data - metadata) / %s", 4 * target.arch.pointerSize.intValue());
		append("\n");
	}

	private void generateMetadata()
	{
		append("metadata:\n");

		for(int i = 0; i != resources.size(); ++i)
		{
			generateInt("key%s - key%s", i + 1, i);
			generateInt("key%s", i);
			generateInt("res%s - res%s", i + 1, i);
			generateInt("res%s", i);
		}

		append("\n");
	}

	private void generateData()
	{
		append("data:\n");

		int i = 0;
		for(String key : resources.keySet())
			append("key%s: .ascii %s\n", i++, escape(key));

		append("key%s:\n", i);

		int j = 0;
		for(Path resourcePath : resources.values())
			append("res%s: .incbin %s\n", j++, escape(resourcePath.toString()));

		append("res%s:\n", j);
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
		ResourceGenerator generator = new ResourceGenerator(target, resources);
		FileUtils.writeFile(generator.generate(), outputPath);
	}
}
