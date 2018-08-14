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

public class DiagnosticId
{
	public final DiagnosticKind kind;
	public final int number;
	public final String fmt;

	public DiagnosticId(DiagnosticKind kind, int number, String fmt)
	{
		this.kind = kind;
		this.number = number;
		this.fmt = fmt;
	}

	public String name()
	{
		var kindString = kind.toString().substring(0, 3);
		return String.format("%s%03d", kindString, number);
	}

	public String format(DiagnosticType type, Object... args)
	{
		var typeString = type.toString().toLowerCase();
		var message = String.format(fmt, args);
		return String.format("%s %s: %s", typeString, name(), message);
	}
}
