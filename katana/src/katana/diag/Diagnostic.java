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
	private final DiagnosticId id;
	private final DiagnosticType type;
	private final SourceLocation location;
	private final String message;
	private final Maybe<StackTrace> trace;

	public Diagnostic(DiagnosticId id, DiagnosticType type, SourceLocation location, String message, Maybe<StackTrace> trace)
	{
		this.id = id;
		this.type = type;
		this.location = location;
		this.message = message;
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
		String typeString = type.toString().toLowerCase();
		String sourceLine = location.file.line(location.line).trim();
		String indicator = makeLocationIndicator(location);
		String traceString = trace.map(StackTrace::toString).or("");
		return String.format("%s: %s %s: %s\n\t%s\n\t%s\n%s", location, typeString, id, message, sourceLine, indicator, traceString);
	}
}
