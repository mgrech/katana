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
import katana.diag.DiagnosticsManager;
import katana.scanner.Scanner;
import katana.scanner.ScannerState;
import katana.scanner.SourceFile;
import katana.scanner.Token;

public class ParseContext implements Cloneable
{
	private final Scanner scanner;
	private final DiagnosticsManager diag;
	private LateParseExprs lateParseExprs;

	public ParseContext(Scanner scanner, DiagnosticsManager diag)
	{
		this.scanner = scanner;
		this.diag = diag;
		this.lateParseExprs = new LateParseExprs();
	}

	private ParseContext(SourceFile file, ScannerState state, DiagnosticsManager diag, LateParseExprs lateParseExprs)
	{
		this.scanner = new Scanner(file);
		this.scanner.backtrack(state);
		this.diag = diag;
		this.lateParseExprs = lateParseExprs;
	}

	@Override
	public ParseContext clone()
	{
		return new ParseContext(scanner.file(), scanner.capture(), diag, lateParseExprs.clone());
	}

	public void backtrack(ParseContext ctx)
	{
		scanner.backtrack(ctx.scanner.state());
		diag.rewind(diag.amount() - ctx.diag.amount());
		lateParseExprs = ctx.lateParseExprs;
	}

	public LateParseExprs lateParseExprs()
	{
		return lateParseExprs;
	}

	public Token token()
	{
		return scanner.state().token;
	}

	public void advance()
	{
		scanner.advance();
	}

	public void error(String fmt, Object... args)
	{
		diag.error(scanner.location(), fmt, args);
	}

	public void warning(String fmt, Object... args)
	{
		diag.warning(scanner.location(), fmt, args);
	}

	public void note(String fmt, Object... args)
	{
		diag.note(scanner.location(), fmt, args);
	}
}
