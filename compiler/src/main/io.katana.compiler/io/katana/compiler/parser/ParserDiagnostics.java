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

package io.katana.compiler.parser;

import io.katana.compiler.diag.DiagnosticId;
import io.katana.compiler.diag.DiagnosticKind;

public class ParserDiagnostics
{
	public static final DiagnosticId UNEXPECTED_TOKEN                        = new DiagnosticId(DiagnosticKind.SYNTACTIC, 1, "unexpected token '%s'");
	public static final DiagnosticId UNEXPECTED_TOKEN_EXPECTED               = new DiagnosticId(DiagnosticKind.SYNTACTIC, 2, "unexpected token '%s', expected '%s'");
	public static final DiagnosticId FORMING_CONST_FUNCTION_TYPE             = new DiagnosticId(DiagnosticKind.SYNTACTIC, 3, "forming const function type");
	public static final DiagnosticId FORMING_CONST_TUPLE_TYPE                = new DiagnosticId(DiagnosticKind.SYNTACTIC, 4, "forming const tuple type");
	public static final DiagnosticId FORMING_CONST_ARRAY_TYPE                = new DiagnosticId(DiagnosticKind.SYNTACTIC, 5, "forming const array type");
	public static final DiagnosticId DUPLICATE_CONST                         = new DiagnosticId(DiagnosticKind.SYNTACTIC, 6, "duplicate 'const'");
	public static final DiagnosticId UNEXPECTED_CHARACTER_IN_TYPE_QUALIFIERS = new DiagnosticId(DiagnosticKind.SYNTACTIC, 7, "unexpected character '%s' while parsing type qualifiers");
	public static final DiagnosticId SEMICOLON_IS_INVALID_EMPTY_STATEMENT    = new DiagnosticId(DiagnosticKind.SYNTACTIC, 8, "';' does not denote a valid empty statement, use '{}' instead");
}
