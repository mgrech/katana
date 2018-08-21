// Copyright 2017-2018 Markus Grech
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
public class KeywordIdentifierTests
{
	@Test
	public void recognizesKeywords()
	{
		var tok = Tokenization.of(" void#type\npointer_cast ");
		tok.expectToken(1, 4, TokenCategory.TYPE, TokenType.TYPE_VOID, null);
		tok.expectToken(11, 12, TokenCategory.KW, TokenType.KW_POINTER_CAST, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void recognizesIdentifiers()
	{
		var tok = Tokenization.of("foo( bar.baz )voids");
		tok.expectToken(0, 3, TokenCategory.IDENT, TokenType.IDENT, "foo");
		tok.expectToken(3, 1, TokenCategory.PUNCT, TokenType.PUNCT_LPAREN, null);
		tok.expectToken(5, 3, TokenCategory.IDENT, TokenType.IDENT, "bar");
		tok.expectToken(8, 1, TokenCategory.OP, TokenType.OP_INFIX, ".");
		tok.expectToken(9, 3, TokenCategory.IDENT, TokenType.IDENT, "baz");
		tok.expectToken(13, 1, TokenCategory.PUNCT, TokenType.PUNCT_RPAREN, null);
		tok.expectToken(14, 5, TokenCategory.IDENT, TokenType.IDENT, "voids");
		tok.expectNoFurtherTokensOrErrors();
	}
}
