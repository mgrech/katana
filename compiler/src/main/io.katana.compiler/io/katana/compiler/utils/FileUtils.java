// Copyright 2016-2019 Markus Grech
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

package io.katana.compiler.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils
{
	public static void copyDirectory(Path source, Path destination) throws IOException
	{
		Files.walkFileTree(source, new SimpleFileVisitor<>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
				try
				{
					Files.copy(dir, destination.resolve(source.relativize(dir)));
				}
				catch(FileAlreadyExistsException ex)
				{}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
			{
				Files.copy(path, destination.resolve(source.relativize(path)));
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static void deleteDirectory(Path directory) throws IOException
	{
		Files.walkFileTree(directory, new SimpleFileVisitor<>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException
			{
				if(ex != null)
					throw ex;

				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static void writeFile(String content, Path destination) throws IOException
	{
		try(var stream = new FileOutputStream(destination.toFile()))
		{
			stream.write(content.getBytes(StandardCharsets.UTF_8));
		}
	}

	public static String stripExtension(String name)
	{
		var dotpos = name.lastIndexOf('.');

		if(dotpos == -1)
			return name;

		return name.substring(0, dotpos);
	}
}
