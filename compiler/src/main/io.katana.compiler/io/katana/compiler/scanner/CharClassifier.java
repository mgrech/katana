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

package io.katana.compiler.scanner;

public class CharClassifier
{
	public static boolean isWhitespace(int cp)
	{
		return cp == ' ' || cp == '\t' || cp == '\r';
	}

	public static boolean isLineBreak(int cp)
	{
		return cp == '\n';
	}

	public static boolean isDecDigit(int cp)
	{
		return cp >= '0' && cp <= '9';
	}

	public static boolean isHexDigit(int cp)
	{
		return isDigit(cp, 16);
	}

	public static boolean isDigit(int cp, int base)
	{
		var index = "0123456789abcdefghijklmnopqrstuvwxyz".indexOf(Character.toLowerCase(cp));
		return index != -1 && index < base;
	}

	public static boolean isAnyDigit(int cp)
	{
		return isDigit(cp, 36);
	}

	private static boolean isAsciiLetter(int cp)
	{
		return cp >= 'a' && cp <= 'z' || cp >= 'A' && cp <= 'Z';
	}

	public static boolean isIdentifierHead(int cp)
	{
		return isAsciiLetter(cp);
	}

	public static boolean isIdentifierTail(int cp)
	{
		return isIdentifierHead(cp) || isDecDigit(cp) || cp == '_';
	}

	public static boolean isOpChar(int cp)
	{
		return ".:?!=<>+-*/%&|^~".indexOf(cp) != -1;
	}
}
