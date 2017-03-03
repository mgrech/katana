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
import katana.utils.StringUtils;

public class Diagnostic
{
	private final SourceLocation location;
	private final DiagnosticType type;
	private final DiagnosticId id;
	private final Object[] args;
	private final Maybe<StackTrace> trace;

	public Diagnostic(SourceLocation location, DiagnosticType type, DiagnosticId id, Object[] args, Maybe<StackTrace> trace)
	{
		this.location = location;
		this.type = type;
		this.id = id;
		this.args = args;
		this.trace = trace;
	}

	private static String makeLocationIndicator(SourceLocation location)
	{
		String line = StringUtils.rtrim(location.file.line(location.line));
		int lengthPreLTrim = line.length();
		line = StringUtils.ltrim(line);
		int ltrimmedColumns = lengthPreLTrim - line.length();
		int spaces = location.column - ltrimmedColumns;
		return StringUtils.times(spaces, ' ') + StringUtils.times(location.length, '^');
	}

	@Override
	public String toString()
	{
		String sourceLine = location.file.line(location.line).trim();
		String indicator = makeLocationIndicator(location);
		String traceString = trace.map(StackTrace::toString).or("");
		String message = id.format(type, args);
		return String.format("%s: %s\n\t%s\n\t%s\n%s", location, message, sourceLine, indicator, traceString);
	}
}
