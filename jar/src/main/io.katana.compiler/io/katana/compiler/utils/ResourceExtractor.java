// Copyright 2016-2017 Markus Grech
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ResourceExtractor
{
	private static String removeStart(String start, String path)
	{
		if(!path.startsWith(start))
			throw new AssertionError("invalid argument");

		path = path.substring(start.length());

		if(path.startsWith("/"))
			path = path.substring(1);

		return path;
	}

	private static void extractFromJar(String classpath, String source, Path destination) throws IOException
	{
		JarFile jar = new JarFile(classpath);
		Enumeration<JarEntry> entries = jar.entries();

		while(entries.hasMoreElements())
		{
			JarEntry entry = entries.nextElement();

			if(entry.getName().startsWith(source))
			{
				String relativePath = removeStart(source, entry.getName());

				if(!relativePath.isEmpty())
				{
					if(relativePath.endsWith("/"))
						Files.createDirectories(destination.resolve(relativePath));

					else
					{
						try(InputStream stream = jar.getInputStream(entry))
						{
							Files.copy(stream, destination.resolve(relativePath));
						}
					}
				}
			}
		}
	}

	private static void copyFromDirectory(Path classpath, String source, Path destination) throws IOException
	{
		Path sourcePath = classpath.resolve(source);

		Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
				try
				{
					Files.copy(dir, destination.resolve(sourcePath.relativize(dir)));
				}

				catch(FileAlreadyExistsException ex)
				{}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
			{
				Files.copy(path, destination.resolve(sourcePath.relativize(path)));
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static void extract(String source, Path destination) throws IOException
	{
		source = "resources/" + source;

		try
		{
			File classpath = new File(ResourceExtractor.class.getProtectionDomain().getCodeSource().getLocation().toURI());

			// running from jar file
			if(classpath.isFile())
				extractFromJar(classpath.toString(), source, destination);

			// running from ide
			else
				copyFromDirectory(classpath.toPath(), source, destination);

		}

		catch(URISyntaxException ex)
		{
			Rethrow.of(ex);
		}
	}
}
