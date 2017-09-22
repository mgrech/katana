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

import katana.scanner.ScannerDiagnostics;
import katana.scanner.TokenCategory;
import katana.scanner.TokenType;
import katana.testing.scanner.ScannerTests;
import katana.testing.scanner.Tokenization;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(ScannerTests.class)
public class NumericLiteralTest
{
	@Test
	public void acceptsSimpleIntegerLiterals()
	{
		Tokenization tok = Tokenization.of("0 1337");
		tok.expectNoErrors();
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "0", 10);
		tok.expectToken(2, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "1337", 10);
	}

	// disallowed to prevent accidental use of decimal literals where octal was intended
	@Test
	public void rejectsLeadingZeroesWithoutBasePrefix()
	{
		Tokenization tok = Tokenization.of("01 0002");
		tok.expectError(0, ScannerDiagnostics.INVALID_START_IN_NUMERIC_LITERAL, 1);
		tok.expectError(3, ScannerDiagnostics.INVALID_START_IN_NUMERIC_LITERAL, 3);
		tok.expectToken(1, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "1", 10);
		tok.expectToken(6, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "2", 10);
	}

	@Test
	public void acceptsIntegerLiteralsWithBasePrefix()
	{
		Tokenization tok = Tokenization.of("0b101 0o777 0xabc 0xDEF 0b0");
		tok.expectNoErrors();
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "101", 2);
		tok.expectToken(6, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "777", 8);
		tok.expectToken(12, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "abc", 16);
		tok.expectToken(18, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "DEF", 16);
		tok.expectToken(24, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "0", 2);
	}

	@Test
	public void acceptsLeadingZeroesAfterBasePrefix()
	{
		Tokenization tok = Tokenization.of("0b01 0x0001");
		tok.expectNoErrors();
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "1", 2);
		tok.expectToken(5, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "1", 16);
	}
}
