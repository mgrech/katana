package io.katana.compiler.testing.scanner.tests;

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
}
