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

package katana.backend.llvm;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class StringPool
{
	private final Map<String, String> namesByValue = new TreeMap<>();
	private int counter = 0;

	private String generateName()
	{
		return String.format("@.strpool.%s", counter++);
	}

	public String get(String value)
	{
		String name = namesByValue.get(value);

		if(name != null)
			return name;

		name = generateName();
		namesByValue.put(value, name);
		return name;
	}

	private byte[] utf8Encode(int cp)
	{
		StringBuilder builder = new StringBuilder();
		builder.appendCodePoint(cp);
		return builder.toString().getBytes(StandardCharsets.UTF_8);
	}

	private String escapeCharacter(int cp)
	{
		StringBuilder result = new StringBuilder();

		for(byte b : utf8Encode(cp))
			result.append(String.format("\\%02X", b));

		return result.toString();
	}

	private String escape(String s)
	{
		StringBuilder result = new StringBuilder();

		for(int cp : s.codePoints().toArray())
			if(cp == '"' || cp == '\\' || Character.isISOControl(cp))
				result.append(escapeCharacter(cp));
			else
				result.appendCodePoint(cp);

		return result.toString();
	}

	public void generate(StringBuilder builder)
	{
		for(Map.Entry<String, String> entry : namesByValue.entrySet())
		{
			String name = entry.getValue();
			String value = entry.getKey();
			String strfmt = "%s = private unnamed_addr constant [%s x i8] c\"%s\"\n";
			builder.append(String.format(strfmt, name, value.length(), escape(value)));
		}
	}
}
