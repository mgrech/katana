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
public class WhitespaceTest
{
	@Test
	public void skipsWhitespaceCharacters()
	{
		Tokenization tok = Tokenization.of("\ta\r ( ");
		tok.expectNoErrors();
		tok.expectToken(TokenCategory.IDENT, TokenType.IDENT, "a", 1);
		tok.expectToken(TokenCategory.PUNCT, TokenType.PUNCT_LPAREN, "(", 4);
	}

	@Test
	public void skipsComments()
	{
		Tokenization tok = Tokenization.of("#foo\na #bar \n\n#baz\nb");
		tok.expectNoErrors();
		tok.expectToken(TokenCategory.IDENT, TokenType.IDENT, "a", 5);
		tok.expectToken(TokenCategory.IDENT, TokenType.IDENT, "b", 19);
	}

	@Test
	public void skipsLineBreaks()
	{
		Tokenization tok = Tokenization.of("a\nb");
		tok.expectNoErrors();
		tok.expectToken(TokenCategory.IDENT, TokenType.IDENT, "a", 0);
		tok.expectToken(TokenCategory.IDENT, TokenType.IDENT, "b", 2);
	}
}
