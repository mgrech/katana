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

package io.katana.compiler.diag;

import io.katana.compiler.scanner.SourceLocation;
import io.katana.compiler.utils.Maybe;

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

	private void diagnose(SourceLocation location, DiagnosticId id, DiagnosticType type, Object... args)
	{
		diagnostics.add(new Diagnostic(location, type, id, args, buildStackTrace()));
	}

	public void error(SourceLocation location, DiagnosticId id, Object... args)
	{
		successful = false;
		diagnose(location, id, DiagnosticType.ERROR, args);
	}

	public Diagnostic get(int index)
	{
		return diagnostics.get(index);
	}

	public boolean successful()
	{
		return successful;
	}

	public String summary()
	{
		StringBuilder builder = new StringBuilder();

		for(Diagnostic diagnostic : diagnostics)
		{
			builder.append(diagnostic);
			builder.append('\n');
		}

		return builder.toString();
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
