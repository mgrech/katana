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

package io.katana.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Katana
{
	public static final Path HOME = locateHome();
	public static final String VERSION = loadVersion();

	private static Path locateHome()
	{
		String homePath = System.getenv("KATANA_HOME");

		if(homePath == null)
			throw new RuntimeException("KATANA_HOME not found");

		try
		{
			return Paths.get(homePath).toRealPath();
		}
		catch(IOException ex)
		{
			throw new IOError(ex);
		}
	}

	private static String loadVersion()
	{
		try
		{
			byte[] version = Files.readAllBytes(HOME.resolve("build.txt"));
			Properties properties = new Properties();
			properties.load(new ByteArrayInputStream(version));
			return properties.getProperty("version");
		}
		catch(IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
