// Copyright 2018 Markus Grech
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
import io.katana.compiler.testing.scanner.Tokenization;
import org.junit.Test;

public class OperatorTest
{
	@Test
	public void acceptsInfixOps()
	{
		var tok = Tokenization.of("a.b*c !?*& d");
		tok.expectToken(0, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(1, TokenCategory.OP, TokenType.OP_INFIX, ".");
		tok.expectToken(2, TokenCategory.IDENT, TokenType.IDENT, "b");
		tok.expectToken(3, TokenCategory.OP, TokenType.OP_INFIX, "*");
		tok.expectToken(4, TokenCategory.IDENT, TokenType.IDENT, "c");
		tok.expectToken(6, TokenCategory.OP, TokenType.OP_INFIX, "!?*&");
		tok.expectToken(11, TokenCategory.IDENT, TokenType.IDENT, "d");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsPrefixOpSeqs()
	{
		var tok = Tokenization.of(" *(++a)");
		tok.expectToken(1, TokenCategory.OP, TokenType.OP_PREFIX_SEQ, "*");
		tok.expectToken(2, TokenCategory.PUNCT, TokenType.PUNCT_LPAREN, "(");
		tok.expectToken(3, TokenCategory.OP, TokenType.OP_PREFIX_SEQ, "++");
		tok.expectToken(5, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(6, TokenCategory.PUNCT, TokenType.PUNCT_RPAREN, ")");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsPostfixOpSeqs()
	{
		var tok = Tokenization.of("(a!!)? ");
		tok.expectToken(0, TokenCategory.PUNCT, TokenType.PUNCT_LPAREN, "(");
		tok.expectToken(1, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(2, TokenCategory.OP, TokenType.OP_POSTFIX_SEQ, "!!");
		tok.expectToken(4, TokenCategory.PUNCT, TokenType.PUNCT_RPAREN, ")");
		tok.expectToken(5, TokenCategory.OP, TokenType.OP_POSTFIX_SEQ, "?");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsMixedOpExprs()
	{
		var tok = Tokenization.of(" a+ + +a ");
		tok.expectToken(1, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(2, TokenCategory.OP, TokenType.OP_POSTFIX_SEQ, "+");
		tok.expectToken(4, TokenCategory.OP, TokenType.OP_INFIX, "+");
		tok.expectToken(6, TokenCategory.OP, TokenType.OP_PREFIX_SEQ, "+");
		tok.expectToken(7, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsPrefixOpAtStartOfFile()
	{
		var tok = Tokenization.of("*a");
		tok.expectToken(0, TokenCategory.OP, TokenType.OP_PREFIX_SEQ, "*");
		tok.expectToken(1, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsPostfixOpAtEndOfFile()
	{
		var tok = Tokenization.of("a*");
		tok.expectToken(0, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectToken(1, TokenCategory.OP, TokenType.OP_POSTFIX_SEQ, "*");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsInfixOpAtStartOfFile()
	{
		var tok = Tokenization.of("+ a");
		tok.expectToken(0, TokenCategory.OP, TokenType.OP_INFIX, "+");
		tok.expectToken(2, TokenCategory.IDENT, TokenType.IDENT, "a");
		tok.expectNoFurtherTokensOrErrors();
	}

	@Test
	public void acceptsFileWithOpOnly()
	{
		var tok = Tokenization.of("*");
		tok.expectToken(0, TokenCategory.OP, TokenType.OP_INFIX, "*");
		tok.expectNoFurtherTokensOrErrors();
	}
}
