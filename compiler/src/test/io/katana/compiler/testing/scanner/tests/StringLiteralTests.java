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

public class StringLiteralTests
{
	@Test
	public void acceptsEmptyString()
	{
		var tok = Tokenization.of("\"\"");
		tok.expectToken(0, 2, TokenCategory.LIT, TokenType.LIT_STRING, "");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsSimpleString()
	{
		var tok = Tokenization.of("\"hello!\"");
		tok.expectToken(0, 8, TokenCategory.LIT, TokenType.LIT_STRING, "hello!");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsUnterminatedString()
	{
		var tok = Tokenization.of("\"oops\nabc");
		tok.expectError(0, 5, ScannerDiagnostics.UNTERMINATED_STRING);
		tok.expectToken(0, 5, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectToken(6, 3, TokenCategory.IDENT, TokenType.IDENT, "abc");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsSimpleEscapeSequences()
	{
		var tok = Tokenization.of("\"foobar\\r\\n\"");
		tok.expectToken(0, 12, TokenCategory.LIT, TokenType.LIT_STRING, "foobar\r\n");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsInvalidSimpleEscapeSequence()
	{
		var tok = Tokenization.of("\"\\q\"");
		tok.expectError(1, 2, ScannerDiagnostics.INVALID_ESCAPE);
		tok.expectToken(0, 4, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffSimpleEscapeSequence()
	{
		// "\" is interpreted as an unterminated string with an embedded " character
		var tok = Tokenization.of("\"\\\"");
		tok.expectError(0, 3, ScannerDiagnostics.UNTERMINATED_STRING);
		tok.expectToken(0, 3, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffSimpleEscapeSequenceAtEof()
	{
		var tok = Tokenization.of("\"\\");
		tok.expectError(0, 2, ScannerDiagnostics.UNTERMINATED_STRING);
		tok.expectToken(0, 2, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsHexEscapeSequence()
	{
		var tok = Tokenization.of("\"\\x61\"");
		tok.expectToken(0, 6, TokenCategory.LIT, TokenType.LIT_STRING, "a");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsHexEscapeWithInvalidDigit()
	{
		var tok = Tokenization.of("\"\\xzz\"");
		tok.expectError(3, 1, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE);
		tok.expectError(4, 1, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE);
		tok.expectToken(0, 6, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffHexEscapeSequence()
	{
		var tok = Tokenization.of("\"\\x0\"");
		tok.expectError(1, 3, ScannerDiagnostics.HEX_ESCAPE_TOO_SHORT);
		tok.expectToken(0, 5, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffHexEscapeSequenceAtEof()
	{
		var tok = Tokenization.of("\"\\x");
		tok.expectError(0, 3, ScannerDiagnostics.UNTERMINATED_STRING);
		tok.expectToken(0, 3, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsSmallUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\uA90C\"");
		tok.expectToken(0, 8, TokenCategory.LIT, TokenType.LIT_STRING, "\uA90C");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffShortUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\u123\"");
		tok.expectError(1, 5, ScannerDiagnostics.UNICODE_ESCAPE_TOO_SHORT);
		tok.expectToken(0, 7, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffShortUnicodeEscapeSequenceAtEof()
	{
		var tok = Tokenization.of("\"\\u123");
		tok.expectError(0, 6, ScannerDiagnostics.UNTERMINATED_STRING);
		tok.expectToken(0, 6, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsShortUnicodeEscapeSequenceWithInvalidDigit()
	{
		var tok = Tokenization.of("\"\\uz1y5\"");
		tok.expectError(3, 1, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE);
		tok.expectError(5, 1, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE);
		tok.expectToken(0, 8, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsBigUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\U1037AC\"");
		var expected = new StringBuilder().appendCodePoint(0x1037AC).toString();
		tok.expectToken(0, 10, TokenCategory.LIT, TokenType.LIT_STRING, expected);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffBigUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\U12345\"");
		tok.expectError(1, 7, ScannerDiagnostics.UNICODE_ESCAPE_TOO_SHORT);
		tok.expectToken(0, 9, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffBigUnicodeEscapeSequenceAtEof()
	{
		var tok = Tokenization.of("\"\\U12345");
		tok.expectError(0, 8, ScannerDiagnostics.UNTERMINATED_STRING);
		tok.expectToken(0, 8, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsBigUnicodeEscapeSequenceWithInvalidDigit()
	{
		var tok = Tokenization.of("\"\\Uz1xy00\"");
		tok.expectError(3, 1, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE);
		tok.expectError(5, 1, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE);
		tok.expectError(6, 1, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE);
		tok.expectToken(0, 10, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsOutOfRangeBigUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\U110000\"");
		tok.expectError(1, 8, ScannerDiagnostics.CODEPOINT_OUT_OF_RANGE);
		tok.expectToken(0, 10, TokenCategory.LIT, TokenType.LIT_STRING, null);
		tok.expectNoFurtherTokensOrErrors();
	}
}
