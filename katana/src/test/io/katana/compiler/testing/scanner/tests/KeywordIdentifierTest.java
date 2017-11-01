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
import io.katana.compiler.testing.scanner.ScannerTests;
import io.katana.compiler.testing.scanner.Tokenization;
import io.katana.compiler.scanner.TokenType;
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
		tok.expectToken(1, TokenCategory.TYPE, TokenType.TYPE_VOID, "void");
		tok.expectToken(11, TokenCategory.MISC, TokenType.MISC_POINTER_CAST, "pointer_cast");
	}

	@Test
	public void recognizesIdentifiers()
	{
		Tokenization tok = Tokenization.of("foo( bar.baz )voids");
		tok.expectNoErrors();
		tok.expectToken(0, TokenCategory.IDENT, TokenType.IDENT, "foo");
		tok.expectIgnoreTokens(1);
		tok.expectToken(5, TokenCategory.IDENT, TokenType.IDENT, "bar");
		tok.expectIgnoreTokens(1);
		tok.expectToken(9, TokenCategory.IDENT, TokenType.IDENT, "baz");
		tok.expectIgnoreTokens(1);
		tok.expectToken(14, TokenCategory.IDENT, TokenType.IDENT, "voids");
	}
}
