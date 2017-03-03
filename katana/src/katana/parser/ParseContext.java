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

package katana.parser;

import katana.ast.LateParseExprs;
import katana.diag.DiagnosticId;
import katana.diag.DiagnosticsManager;
import katana.scanner.SourceFile;
import katana.scanner.SourceLocation;
import katana.scanner.Token;

import java.util.List;

public class ParseContext implements Cloneable
{
	private final SourceFile file;
	private final List<Token> tokens;
	private final DiagnosticsManager diag;

	private LateParseExprs lateParseExprs;
	private int current;

	public ParseContext(SourceFile file, List<Token> tokens, DiagnosticsManager diag)
	{
		this(file, tokens, diag, new LateParseExprs(), 0);
	}

	private ParseContext(SourceFile file, List<Token> tokens, DiagnosticsManager diag, LateParseExprs lateParseExprs, int current)
	{
		this.file = file;
		this.tokens = tokens;
		this.diag = diag;
		this.lateParseExprs = lateParseExprs;
		this.current = current;
	}

	private SourceLocation location(int relative)
	{
		int index = current + relative;

		if(index < 0 || index >= tokens.size())
			return null;

		Token token = tokens.get(index);
		return file.resolve(token.offset, token.value.length());
	}

	public SourceFile file()
	{
		return file;
	}

	@Override
	public ParseContext clone()
	{
		return new ParseContext(file, tokens, diag, lateParseExprs.clone(), current);
	}

	public void backtrack(ParseContext ctx)
	{
		current = ctx.current;
		diag.rewind(diag.amount() - ctx.diag.amount());
		lateParseExprs = ctx.lateParseExprs;
	}

	public LateParseExprs lateParseExprs()
	{
		return lateParseExprs;
	}

	public Token token()
	{
		if(current == tokens.size())
			return null;

		return tokens.get(current);
	}

	public void advance()
	{
		++current;
	}

	public void error(DiagnosticId id, Object... args)
	{
		diag.error(location(0), id, args);
	}

	public void error(int relative, DiagnosticId id, Object... args)
	{
		diag.error(location(relative), id, args);
	}

	public void error(SourceLocation location, DiagnosticId id, Object... args)
	{
		diag.error(location, id, args);
	}
}
