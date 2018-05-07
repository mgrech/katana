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
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\"", "");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsSimpleString()
	{
		var tok = Tokenization.of("\"hello!\"");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"hello!\"", "hello!");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsUnterminatedString()
	{
		var tok = Tokenization.of("\"oops\nabc");
		tok.expectError(0, ScannerDiagnostics.UNTERMINATED_STRING, 5);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"oops", null);
		tok.expectToken(6, TokenCategory.IDENT, TokenType.IDENT, "abc");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsSimpleEscapeSequences()
	{
		var tok = Tokenization.of("\"foobar\\r\\n\"");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"foobar\\r\\n\"", "foobar\r\n");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsInvalidSimpleEscapeSequence()
	{
		var tok = Tokenization.of("\"\\q\"");
		tok.expectError(1, ScannerDiagnostics.INVALID_ESCAPE, 2);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\q\"", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffSimpleEscapeSequence()
	{
		// "\" is interpreted as an unterminated string with an embedded " character
		var tok = Tokenization.of("\"\\\"");
		tok.expectError(0, ScannerDiagnostics.UNTERMINATED_STRING, 3);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\\"", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffSimpleEscapeSequenceAtEof()
	{
		var tok = Tokenization.of("\"\\");
		tok.expectError(0, ScannerDiagnostics.UNTERMINATED_STRING, 2);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsHexEscapeSequence()
	{
		var tok = Tokenization.of("\"\\x61\"");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\x61\"", "a");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsHexEscapeWithInvalidDigit()
	{
		var tok = Tokenization.of("\"\\xzz\"");
		tok.expectError(3, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE, 1);
		tok.expectError(4, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE, 1);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\xzz\"", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffHexEscapeSequence()
	{
		var tok = Tokenization.of("\"\\x0\"");
		tok.expectError(1, ScannerDiagnostics.HEX_ESCAPE_TOO_SHORT, 3);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\x0\"", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffHexEscapeSequenceAtEof()
	{
		var tok = Tokenization.of("\"\\x");
		tok.expectError(0, ScannerDiagnostics.UNTERMINATED_STRING, 3);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\x", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsSmallUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\uA90C\"");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\uA90C\"", "\uA90C");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffShortUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\u123\"");
		tok.expectError(1, ScannerDiagnostics.UNICODE_ESCAPE_TOO_SHORT, 5);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\u123\"", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffShortUnicodeEscapeSequenceAtEof()
	{
		var tok = Tokenization.of("\"\\u123");
		tok.expectError(0, ScannerDiagnostics.UNTERMINATED_STRING, 6);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\u123", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsShortUnicodeEscapeSequenceWithInvalidDigit()
	{
		var tok = Tokenization.of("\"\\uz1y5\"");
		tok.expectError(3, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE, 1);
		tok.expectError(5, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE, 1);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\uz1y5\"", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsBigUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\U1037AC\"");
		var expected = new StringBuilder().appendCodePoint(0x1037AC).toString();
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\U1037AC\"", expected);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffBigUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\U12345\"");
		tok.expectError(1, ScannerDiagnostics.UNICODE_ESCAPE_TOO_SHORT, 7);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\U12345\"", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsCutOffBigUnicodeEscapeSequenceAtEof()
	{
		var tok = Tokenization.of("\"\\U12345");
		tok.expectError(0, ScannerDiagnostics.UNTERMINATED_STRING, 8);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\U12345", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsBigUnicodeEscapeSequenceWithInvalidDigit()
	{
		var tok = Tokenization.of("\"\\Uz1xy00\"");
		tok.expectError(3, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE, 1);
		tok.expectError(5, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE, 1);
		tok.expectError(6, ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE, 1);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\Uz1xy00\"", null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsOutOfRangeBigUnicodeEscapeSequence()
	{
		var tok = Tokenization.of("\"\\U110000\"");
		tok.expectError(1, ScannerDiagnostics.CODEPOINT_OUT_OF_RANGE, 8);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_STRING, "\"\\U110000\"", null);
		tok.expectNoFurtherTokensOrErrors();
	}
}
