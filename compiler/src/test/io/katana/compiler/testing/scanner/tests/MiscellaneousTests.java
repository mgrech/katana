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
		tok.expectError(0, ScannerDiagnostics.INVALID_CODEPOINT, 1);
		tok.expectError(4, ScannerDiagnostics.INVALID_CODEPOINT, 1);
		tok.expectToken(1, TokenCategory.IDENT, TokenType.IDENT, "abc");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsLabels()
	{
		var tok = Tokenization.of("hello @foo world");
		tok.expectToken(0, TokenCategory.IDENT, TokenType.IDENT, "hello");
		tok.expectToken(6, TokenCategory.STMT, TokenType.STMT_LABEL, "foo");
		tok.expectToken(11, TokenCategory.IDENT, TokenType.IDENT, "world");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsEmptyLabels()
	{
		var tok = Tokenization.of("@");
		tok.expectError(0, ScannerDiagnostics.EMPTY_LABEL, 1);
		tok.expectToken(0, TokenCategory.STMT, TokenType.STMT_LABEL, null);
		tok.expectNoFurtherTokensOrErrors();
	}
}
