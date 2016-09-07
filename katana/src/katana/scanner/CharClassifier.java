// Copyright 2016 Markus Grech
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

package katana.scanner;

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

	public static boolean isDigit(int cp)
	{
		return cp >= '0' && cp <= '9';
	}

	public static boolean isHexDigit(int cp)
	{
		return isDigit(cp) || (cp >= 'a' && cp <= 'f') || (cp >= 'A' && cp <= 'F');
	}

	public static boolean isIdentifierStart(int cp)
	{
		return cp == '_' || Character.isLetter(cp);
	}

	public static boolean isIdentifierChar(int cp)
	{
		return isIdentifierStart(cp)
		    || Character.isDigit(cp)
		    || Character.getType(cp) == Character.NON_SPACING_MARK
		    || Character.getType(cp) == Character.COMBINING_SPACING_MARK;
	}
}
