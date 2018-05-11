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

import io.katana.compiler.scanner.ScannerDiagnostics;
import io.katana.compiler.scanner.TokenCategory;
import io.katana.compiler.scanner.TokenType;
import io.katana.compiler.testing.scanner.ScannerTests;
import io.katana.compiler.testing.scanner.Tokenization;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(ScannerTests.class)
public class WhitespaceTests
{
	@Test
	public void acceptsWhitespaceCharacters()
	{
		var tok = Tokenization.of("\ta\r ( ");
		tok.expectToken(1, 1, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(4, 1, TokenCategory.PUNCT, TokenType.PUNCT_LPAREN, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsLineBreak()
	{
		var tok = Tokenization.of("a\nb");
		tok.expectToken(0, 1, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(2, 1, TokenCategory.IDENT, TokenType.IDENT, "b");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsSingleLineComments()
	{
		var tok = Tokenization.of("#foo\na #bar \n\n#baz\nb");
		tok.expectToken(5, 1, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(19, 1, TokenCategory.IDENT, TokenType.IDENT, "b");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsEmptySingleLineComments()
	{
		var tok = Tokenization.of("#\n foo #\nbar");
		tok.expectToken(3, 3, TokenCategory.IDENT, TokenType.IDENT, "foo");
		tok.expectToken(9, 3, TokenCategory.IDENT, TokenType.IDENT, "bar");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsEmptyMultiLineComment()
	{
		var tok = Tokenization.of("#{#} foo #{#}bar");
		tok.expectToken(5, 3, TokenCategory.IDENT, TokenType.IDENT, "foo");
		tok.expectToken(13, 3, TokenCategory.IDENT, TokenType.IDENT, "bar");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsMultiLineComment()
	{
		var tok = Tokenization.of("#{ comment #} foo");
		tok.expectToken(14, 3, TokenCategory.IDENT, TokenType.IDENT, "foo");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsNestedMultiLineComments()
	{
		var tok = Tokenization.of("#{ hello #{ world #} ! #} foo");
		tok.expectToken(26, 3, TokenCategory.IDENT, TokenType.IDENT, "foo");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsMultiLineCommentWithEmbeddedSingleLineComment()
	{
		var tok = Tokenization.of("#{ #foo #} foo");
		tok.expectToken(11, 3, TokenCategory.IDENT, TokenType.IDENT, "foo");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsUnterminatedMultiLineComment()
	{
		var tok = Tokenization.of("#{");
		tok.expectError(0, 2, ScannerDiagnostics.UNTERMINATED_MULTILINE_COMMENT);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsHalfUnterminatedMultLineComment()
	{
		var tok = Tokenization.of("#{#");
		tok.expectError(0, 3, ScannerDiagnostics.UNTERMINATED_MULTILINE_COMMENT);
		tok.expectNoFurtherTokensOrErrors();
	}
}
