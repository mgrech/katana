// Copyright 2017-2019 Markus Grech
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
import io.katana.compiler.utils.StringUtils;

public class Diagnostic
{
	public final SourceLocation location;
	public final DiagnosticType type;
	public final DiagnosticId id;
	public final Object[] args;
	public final Maybe<StackTrace> trace;
	private final String asString;

	public Diagnostic(SourceLocation location, DiagnosticType type, DiagnosticId id, Object[] args, Maybe<StackTrace> trace)
	{
		this.location = location;
		this.type = type;
		this.id = id;
		this.args = args;
		this.trace = trace;
		this.asString = asString();
	}

	private String asString()
	{
		var sourceLine = location.file.line(location.line).trim();
		var indicator = makeLocationIndicator(location);
		var traceString = trace.map(StackTrace::toString).or("");
		var message = id.format(type, args);
		return String.format("%s: %s\n\t%s\n\t%s\n%s", location, message, sourceLine, indicator, traceString);
	}

	private static String makeLocationIndicator(SourceLocation location)
	{
		var line = StringUtils.rtrim(location.file.line(location.line));
		var lengthPreLTrim = line.length();
		line = StringUtils.ltrim(line);
		var ltrimmedColumns = lengthPreLTrim - line.length();
		var spaces = location.column - ltrimmedColumns;
		return StringUtils.times(spaces, ' ') + StringUtils.times(location.length, '^');
	}

	@Override
	public String toString()
	{
		return asString;
	}
}
