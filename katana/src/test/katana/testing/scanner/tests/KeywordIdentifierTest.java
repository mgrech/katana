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

package katana.testing.scanner.tests;

import katana.scanner.TokenCategory;
import katana.scanner.TokenType;
import katana.testing.scanner.ScannerTests;
import katana.testing.scanner.Tokenization;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(ScannerTests.class)
public class KeywordIdentifierTest
{
	@Test
	public void recognizesKeywords()
	{
		Tokenization tok = Tokenization.of(" void#type\npointer_cast ");
		tok.expectNoErrors();
		tok.expectToken(TokenCategory.TYPE, TokenType.TYPE_VOID, "void", 1);
		tok.expectToken(TokenCategory.MISC, TokenType.MISC_POINTER_CAST, "pointer_cast", 11);
	}

	@Test
	public void recognizesIdentifiers()
	{
		Tokenization tok = Tokenization.of("foo( bar.baz )voids");
		tok.expectNoErrors();
		tok.expectToken(TokenCategory.IDENT, TokenType.IDENT, "foo", 0);
		tok.expectIgnoreTokens(1);
		tok.expectToken(TokenCategory.IDENT, TokenType.IDENT, "bar", 5);
		tok.expectIgnoreTokens(1);
		tok.expectToken(TokenCategory.IDENT, TokenType.IDENT, "baz", 9);
		tok.expectIgnoreTokens(1);
		tok.expectToken(TokenCategory.IDENT, TokenType.IDENT, "voids", 14);
	}
}
