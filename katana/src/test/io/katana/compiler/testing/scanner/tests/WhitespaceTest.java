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

package io.katana.compiler.testing.scanner.tests;

import io.katana.compiler.scanner.TokenCategory;
import io.katana.compiler.scanner.TokenType;
import io.katana.compiler.testing.scanner.ScannerTests;
import io.katana.compiler.testing.scanner.Tokenization;
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
		tok.expectToken(1, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(4, TokenCategory.PUNCT, TokenType.PUNCT_LPAREN, "(");
	}

	@Test
	public void skipsComments()
	{
		Tokenization tok = Tokenization.of("#foo\na #bar \n\n#baz\nb");
		tok.expectNoErrors();
		tok.expectToken(5, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(19, TokenCategory.IDENT, TokenType.IDENT, "b");
	}

	@Test
	public void skipsLineBreaks()
	{
		Tokenization tok = Tokenization.of("a\nb");
		tok.expectNoErrors();
		tok.expectToken(0, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(2, TokenCategory.IDENT, TokenType.IDENT, "b");
	}
}
