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

package io.katana.compiler.scanner;

import io.katana.compiler.diag.DiagnosticId;
import io.katana.compiler.diag.DiagnosticKind;

public class ScannerDiagnostics
{
	public static final DiagnosticId INVALID_CODEPOINT                = new DiagnosticId(DiagnosticKind.LEXICAL,  1, "invalid codepoint encountered: %s");
	public static final DiagnosticId UNTERMINATED_MULTILINE_COMMENT   = new DiagnosticId(DiagnosticKind.LEXICAL, 2, "unterminated multi-line comment");

	public static final DiagnosticId UNTERMINATED_STRING              = new DiagnosticId(DiagnosticKind.LEXICAL,  3, "unterminated string literal");
	public static final DiagnosticId INVALID_ESCAPE                   = new DiagnosticId(DiagnosticKind.LEXICAL,  4, "invalid escape sequence %s");
	public static final DiagnosticId INVALID_CHARACTER_IN_ESCAPE      = new DiagnosticId(DiagnosticKind.LEXICAL,  5, "expected hex digits in escape sequence");
	public static final DiagnosticId CODEPOINT_OUT_OF_RANGE           = new DiagnosticId(DiagnosticKind.LEXICAL,  6, "codepoint in unicode escape sequence is out of range");
	public static final DiagnosticId HEX_ESCAPE_TOO_SHORT             = new DiagnosticId(DiagnosticKind.LEXICAL,  7, "hex escape sequence is too short, requires exactly 2 digits");
	public static final DiagnosticId UNICODE_ESCAPE_TOO_SHORT         = new DiagnosticId(DiagnosticKind.LEXICAL,  8, "unicode escape sequence is too short, requires exactly %s digits");

	public static final DiagnosticId INVALID_START_IN_NUMERIC_LITERAL = new DiagnosticId(DiagnosticKind.LEXICAL,  9, "numeric literal must start with digit 1-9 or base prefix");
	public static final DiagnosticId EMPTY_NUMERIC_LITERAL            = new DiagnosticId(DiagnosticKind.LEXICAL, 10, "numeric literal requires at least one digit");
	public static final DiagnosticId INVALID_LITERAL_SUFFIX           = new DiagnosticId(DiagnosticKind.LEXICAL, 11, "invalid literal suffix '%s'");
	public static final DiagnosticId BASE_PREFIX_ON_FLOAT_LITERAL     = new DiagnosticId(DiagnosticKind.LEXICAL, 12, "base prefixes are not supported with floating point literals");
	public static final DiagnosticId INT_SUFFIX_ON_FLOAT_LITERAL      = new DiagnosticId(DiagnosticKind.LEXICAL, 13, "integer suffix used on floating point literal");
	public static final DiagnosticId INVALID_DIGIT_FOR_BASE           = new DiagnosticId(DiagnosticKind.LEXICAL, 14, "invalid digit for numeric literal with base %s");
	public static final DiagnosticId EMPTY_SUFFIX                     = new DiagnosticId(DiagnosticKind.LEXICAL, 15, "empty literal suffix");

	public static final DiagnosticId EMPTY_LABEL                      = new DiagnosticId(DiagnosticKind.LEXICAL, 16, "label name is empty");
}
