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

package katana.diag;

import katana.scanner.SourceLocation;
import katana.utils.Maybe;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticsManager
{
	private final boolean stackTraces;
	private final List<Diagnostic> diagnostics = new ArrayList<>();
	private boolean successful = true;

	public DiagnosticsManager(boolean stackTraces)
	{
		this.stackTraces = stackTraces;
	}

	private Maybe<StackTrace> buildStackTrace()
	{
		if(!stackTraces)
			return Maybe.none();

		StackTrace trace = StackTrace.get();

		// trim stack frames from this class:
		// error/warning/note -> diagnose -> buildStackTrace
		for(int i = 0; i != 3; ++i)
			trace.trimInnermostFrame();

		return Maybe.some(trace);
	}

	private void diagnose(DiagnosticId id, DiagnosticType type, SourceLocation location, String fmt, Object... args)
	{
		diagnostics.add(new Diagnostic(id, type, location, String.format(fmt, args), buildStackTrace()));
	}

	public void error(DiagnosticId id, SourceLocation location, String fmt, Object... args)
	{
		successful = false;
		diagnose(id, DiagnosticType.ERROR, location, fmt, args);
	}

	public void warning(DiagnosticId id, SourceLocation location, String fmt, Object... args)
	{
		diagnose(id, DiagnosticType.WARNING, location, fmt, args);
	}

	public void note(DiagnosticId id, SourceLocation location, String fmt, Object... args)
	{
		diagnose(id, DiagnosticType.NOTE, location, fmt, args);
	}

	public boolean successful()
	{
		return successful;
	}

	public void print()
	{
		for(Diagnostic diagnostic : diagnostics)
			System.err.println(diagnostic);
	}

	public void rewind(int amount)
	{
		diagnostics.subList(diagnostics.size() - amount, diagnostics.size()).clear();
	}

	public int amount()
	{
		return diagnostics.size();
	}
}
