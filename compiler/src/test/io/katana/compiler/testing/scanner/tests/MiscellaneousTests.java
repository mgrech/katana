// Copyright 2018-2019 Markus Grech
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
import io.katana.compiler.testing.scanner.Tokenization;
import org.junit.Test;

public class MiscellaneousTests
{
	@Test
	public void acceptsEmptyFile()
	{
		var tok = Tokenization.of("");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsInvalidCodepoints()
	{
		var tok = Tokenization.of("\fabc\u1234");
		tok.expectError(0, 1, ScannerDiagnostics.INVALID_CODEPOINT);
		tok.expectError(4, 1, ScannerDiagnostics.INVALID_CODEPOINT);
		tok.expectToken(1, 3, TokenCategory.IDENT, TokenType.IDENT, "abc");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsLabels()
	{
		var tok = Tokenization.of("hello @foo world");
		tok.expectToken(0, 5, TokenCategory.IDENT, TokenType.IDENT, "hello");
		tok.expectToken(6, 4, TokenCategory.LABEL, TokenType.LABEL, "foo");
		tok.expectToken(11, 5, TokenCategory.IDENT, TokenType.IDENT, "world");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsEmptyLabels()
	{
		var tok = Tokenization.of("@");
		tok.expectError(0, 1, ScannerDiagnostics.EMPTY_LABEL);
		tok.expectToken(0, 1, TokenCategory.LABEL, TokenType.LABEL, null);
		tok.expectNoFurtherTokensOrErrors();
	}
}
