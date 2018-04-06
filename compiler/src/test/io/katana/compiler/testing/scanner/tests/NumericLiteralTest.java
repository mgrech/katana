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
public class NumericLiteralTest
{
	@Test
	public void acceptsSimpleIntegerLiterals()
	{
		var tok = Tokenization.of("0 1337");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "0", 10);
		tok.expectToken(2, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "1337", 10);
		tok.expectNoFurtherTokensOrErrors();
	}

	// disallowed to prevent accidental use of decimal literals where octal was intended
	@Test
	public void rejectsLeadingZeroesWithoutBasePrefix()
	{
		var tok = Tokenization.of("01 0002");
		tok.expectError(0, ScannerDiagnostics.INVALID_START_IN_NUMERIC_LITERAL, 1);
		tok.expectError(3, ScannerDiagnostics.INVALID_START_IN_NUMERIC_LITERAL, 3);
		tok.expectToken(1, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "1", 10);
		tok.expectToken(6, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "2", 10);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsIntegerLiteralsWithBasePrefix()
	{
		var tok = Tokenization.of("0b101 0o777 0xabc 0xDEF 0b0");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "101", 2);
		tok.expectToken(6, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "777", 8);
		tok.expectToken(12, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "abc", 16);
		tok.expectToken(18, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "DEF", 16);
		tok.expectToken(24, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "0", 2);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsLeadingZeroesAfterBasePrefix()
	{
		var tok = Tokenization.of("0b01 0x0001");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "1", 2);
		tok.expectToken(5, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "1", 16);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsIntegerLiteralsWithBasePrefixOnly()
	{
		var tok = Tokenization.of("0o 0b");
		tok.expectError(0, ScannerDiagnostics.EMPTY_NUMERIC_LITERAL, 2);
		tok.expectError(3, ScannerDiagnostics.EMPTY_NUMERIC_LITERAL, 2);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, null, 8);
		tok.expectToken(3, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, null, 2);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsIntegerLiteralWithOutOfRangeDigit()
	{
		var tok = Tokenization.of("0b1012 0o4585 0xghr");
		tok.expectError(5, ScannerDiagnostics.INVALID_DIGIT_FOR_BASE, 1);
		tok.expectError(11, ScannerDiagnostics.INVALID_DIGIT_FOR_BASE, 1);
		tok.expectError(16, ScannerDiagnostics.INVALID_DIGIT_FOR_BASE, 1);
		tok.expectError(17, ScannerDiagnostics.INVALID_DIGIT_FOR_BASE, 1);
		tok.expectError(18, ScannerDiagnostics.INVALID_DIGIT_FOR_BASE, 1);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, null);
		tok.expectToken(7, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, null);
		tok.expectToken(14, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsFloatingPointLiterals()
	{
		var tok = Tokenization.of("0.0 123. .123 123.123");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_FLOAT_DEDUCE, "0.0");
		tok.expectToken(4, TokenCategory.LIT, TokenType.LIT_FLOAT_DEDUCE, "123.");
		tok.expectToken(9, TokenCategory.LIT, TokenType.LIT_FLOAT_DEDUCE, ".123");
		tok.expectToken(14, TokenCategory.LIT, TokenType.LIT_FLOAT_DEDUCE, "123.123");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsFloatingPointLiteralWithBasePrefix()
	{
		var tok = Tokenization.of("0b1.0 0x2.0 0o3.0");
		tok.expectError(0, ScannerDiagnostics.BASE_PREFIX_ON_FLOAT_LITERAL, 2);
		tok.expectError(6, ScannerDiagnostics.BASE_PREFIX_ON_FLOAT_LITERAL, 2);
		tok.expectError(12, ScannerDiagnostics.BASE_PREFIX_ON_FLOAT_LITERAL, 2);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_FLOAT_DEDUCE, null);
		tok.expectToken(6, TokenCategory.LIT, TokenType.LIT_FLOAT_DEDUCE, null);
		tok.expectToken(12, TokenCategory.LIT, TokenType.LIT_FLOAT_DEDUCE, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsSuffixes()
	{
		var tok = Tokenization.of("12$i 1337$u16 7.5$f64");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT, "12");
		tok.expectToken(5, TokenCategory.LIT, TokenType.LIT_UINT16, "1337");
		tok.expectToken(14, TokenCategory.LIT, TokenType.LIT_FLOAT64, "7.5");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsInvalidSuffixes()
	{
		var tok = Tokenization.of("0$abc");
		tok.expectError(2, ScannerDiagnostics.INVALID_LITERAL_SUFFIX, 3);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsEmptySuffix()
	{
		var tok = Tokenization.of("0$ 123.4$");
		tok.expectError(1, ScannerDiagnostics.EMPTY_SUFFIX, 1);
		tok.expectError(8, ScannerDiagnostics.EMPTY_SUFFIX, 1);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, null);
		tok.expectToken(3, TokenCategory.LIT, TokenType.LIT_FLOAT_DEDUCE, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void rejectsIntSuffixOnFloatLiteral()
	{
		var tok = Tokenization.of("123.456$i64");
		tok.expectError(8, ScannerDiagnostics.INT_SUFFIX_ON_FLOAT_LITERAL, 3);
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_FLOAT_DEDUCE, null);
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsDigitSeparators()
	{
		var tok = Tokenization.of("1'2''3 0x1' 0b'100'$i32");
		tok.expectToken(0, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "123", 10);
		tok.expectToken(7, TokenCategory.LIT, TokenType.LIT_INT_DEDUCE, "1", 16);
		tok.expectToken(12, TokenCategory.LIT, TokenType.LIT_INT32, "100", 2);
		tok.expectNoFurtherTokensOrErrors();
	}
}
