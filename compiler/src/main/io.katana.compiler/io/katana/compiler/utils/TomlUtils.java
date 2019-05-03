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

package io.katana.compiler.utils;

import io.ous.jtoml.JToml;
import io.ous.jtoml.TomlTable;

import java.io.IOException;
import java.nio.file.Path;

public class TomlUtils
{
	private static String renameKey(String key)
	{
		var parts = key.split("-");
		var result = new StringBuilder();

		result.append(parts[0]);

		for(var i = 1; i != parts.length; ++i)
		{
			result.append(Character.toUpperCase(parts[i].charAt(0)));
			result.append(parts[i].substring(1));
		}

		return result.toString();
	}

	private static TomlTable renameFields(TomlTable toml)
	{
		var result = new TomlTable();

		for(var entry : toml.entrySet())
		{
			var key = entry.getKey();
			var value = entry.getValue();

			if(value instanceof TomlTable)
				result.put(key, renameFields((TomlTable)value));
			else
				result.put(renameKey(key), value);
		}

		return result;
	}

	public static TomlTable loadToml(Path path) throws IOException
	{
		return renameFields(JToml.parse(path.toFile()));
	}
}
