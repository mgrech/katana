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

package io.katana.compiler.testing.scanner;

import io.katana.compiler.diag.*;
import io.katana.compiler.scanner.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Tokenization
{
	private static final Path HERE = Paths.get("");

	private final List<Token> tokens;
	private final DiagnosticsManager diag;
	private int currentToken = 0;
	private int currentDiagnostic = 0;

	private Tokenization(List<Token> tokens, DiagnosticsManager diag)
	{
		this.tokens = tokens;
		this.diag = diag;
	}

	public static Tokenization of(String s)
	{
		SourceFile file = SourceFile.fromBytes(HERE, HERE, s.getBytes(StandardCharsets.UTF_8));
		DiagnosticsManager diag = new DiagnosticsManager(true);
		List<Token> tokens = Scanner.tokenize(file, diag);
		return new Tokenization(tokens, diag);
	}

	public void expectNoErrors()
	{
		if(!diag.successful())
			throw new CompileException(diag.summary());
	}

	public void expectError(int offset, DiagnosticId id, int length)
	{
		Diagnostic msg = diag.get(currentDiagnostic++);
		assertEquals("wrong diagnostic type", DiagnosticType.ERROR, msg.type);
		assertEquals("wrong diagnostic id",     id, msg.id);
		assertEquals("wrong diagnostic offset", offset, msg.location.offset);
		assertEquals("wrong diagnostic length", length, msg.location.length);
	}

	public void expectIgnoreTokens(int n)
	{
		for(int i = 0; i != n; ++i)
			tokens.get(currentToken + i);

		currentToken += n;
	}

	public void expectToken(int offset, TokenCategory category, TokenType type, String value)
	{
		Token token = tokens.get(currentToken++);
		assertEquals("wrong token offset",   offset, token.offset);
		assertEquals("wrong token category", category, token.category);
		assertEquals("wrong token type",     type, token.type);
		assertEquals("wrong token value",    value, token.value);
	}

	public void expectToken(int offset, TokenCategory category, TokenType type, String value, Object data)
	{
		assertEquals("wrong token data", data, tokens.get(currentToken).data);
		expectToken(offset, category, type, value);
	}
}
