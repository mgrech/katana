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

import io.katana.compiler.scanner.CharClassifier;

public class StringUtils
{
	public static String ltrim(String s)
	{
		while(!s.isEmpty() && CharClassifier.isWhitespace(s.charAt(0)))
			s = s.substring(1);

		return s;
	}

	public static String rtrim(String s)
	{
		while(!s.isEmpty() && CharClassifier.isWhitespace(s.charAt(s.length() - 1)))
			s = s.substring(0, s.length() - 1);

		return s;
	}

	public static String times(int count, char c)
	{
		assert count >= 0 : "negative count";

		var builder = new StringBuilder();

		for(var i = 0; i != count; ++i)
			builder.append(c);

		return builder.toString();
	}

	public static String formatCodepoint(int cp)
	{
		assert cp >= 0 && cp <= 0x10FFFF : "invalid codepoint";

		var name = String.format("{%s}", Character.getName(cp));

		if(cp <= 0xFFFF)
			return String.format("U+%04X %s", cp, name);

		return String.format("U+%X %s", cp, name);
	}
}
