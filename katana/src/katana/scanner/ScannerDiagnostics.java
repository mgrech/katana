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

package katana.scanner;

import katana.diag.DiagnosticId;
import katana.diag.DiagnosticKind;

public class ScannerDiagnostics
{
	public static final DiagnosticId INVALID_CODEPOINT                   = new DiagnosticId(DiagnosticKind.LEXICAL,  1);
	public static final DiagnosticId UNTERMINATED_STRING                 = new DiagnosticId(DiagnosticKind.LEXICAL,  2);
	public static final DiagnosticId INVALID_ESCAPE_SEQUENCE             = new DiagnosticId(DiagnosticKind.LEXICAL,  3);
	public static final DiagnosticId INVALID_CHARACTER_IN_UNICODE_ESCAPE = new DiagnosticId(DiagnosticKind.LEXICAL,  4);
	public static final DiagnosticId INVALID_CODEPOINT_IN_UNICODE_ESCAPE = new DiagnosticId(DiagnosticKind.LEXICAL,  5);
	public static final DiagnosticId INVALID_CHARACTER_IN_HEX_ESCAPE     = new DiagnosticId(DiagnosticKind.LEXICAL,  6);
	public static final DiagnosticId INVALID_START_IN_NUMERIC_LITERAL    = new DiagnosticId(DiagnosticKind.LEXICAL,  7);
	public static final DiagnosticId EMPTY_NUMERIC_LITERAL               = new DiagnosticId(DiagnosticKind.LEXICAL,  8);
	public static final DiagnosticId UNKNOWN_SUFFIX_IN_NUMERIC_LITERAL   = new DiagnosticId(DiagnosticKind.LEXICAL,  9);
	public static final DiagnosticId BASE_PREFIX_ON_FLOAT_LITERAL        = new DiagnosticId(DiagnosticKind.LEXICAL, 10);
	public static final DiagnosticId INT_SUFFIX_ON_FLOAT_LITERAL         = new DiagnosticId(DiagnosticKind.LEXICAL, 11);
}
