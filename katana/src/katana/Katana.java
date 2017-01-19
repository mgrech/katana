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

package katana;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Katana
{
	private static final String VERSION_STRING = loadVersion();

	private static String loadVersion()
	{
		try
		{
			InputStream is = Katana.class.getClassLoader().getResourceAsStream("resources/version.txt");
			Properties properties = new Properties();
			properties.load(is);
			return properties.getProperty("version");
		}

		catch(IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}

	private static Path locateHome()
	{
		String homePath = System.getenv("KATANA_HOME");

		if(homePath == null)
			throw new RuntimeException("KATANA_HOME not found");

		return Paths.get(homePath);
	}

	public static String version()
	{
		return VERSION_STRING;
	}

	public static final String FILE_EXTENSION_KATANA  = ".ks";
	public static final String FILE_EXTENSION_C       = ".c";
	public static final String FILE_EXTENSION_CPP     = ".cpp";
	public static final String FILE_EXTENSION_ASM     = ".asm";

	public static final Path HOME = locateHome();
}
