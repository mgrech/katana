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

package io.katana.compiler.scanner;

public enum TokenType
{
	KW_ABI,
	KW_ALIGNOF,
	KW_BUILTIN,
	KW_DATA,
	KW_ELSE,
	KW_EXPORT,
	KW_EXTERN,
	KW_FN,
	KW_GLOBAL,
	KW_GOTO,
	KW_IF,
	KW_IMPORT,
	KW_INLINE,
	KW_LOOP,
	KW_MODULE,
	KW_NARROW_CAST,
	KW_OFFSETOF,
	KW_OPAQUE,
	KW_OPERATOR,
	KW_POINTER_CAST,
	KW_RETURN,
	KW_SIGN_CAST,
	KW_SIZEOF,
	KW_TYPE,
	KW_UNDEF,
	KW_UNLESS,
	KW_UNREACHABLE,
	KW_UNTIL,
	KW_VAR,
	KW_WHILE,
	KW_WIDEN_CAST,

	IDENT,
	LABEL,

	LIT_BOOL,
	LIT_FLOAT_DEDUCE,
	LIT_FLOAT32,
	LIT_FLOAT64,
	LIT_INT_DEDUCE,
	LIT_INT,
	LIT_INT8,
	LIT_INT16,
	LIT_INT32,
	LIT_INT64,
	LIT_NULL,
	LIT_STRING,
	LIT_UINT,
	LIT_UINT8,
	LIT_UINT16,
	LIT_UINT32,
	LIT_UINT64,

	OP_INFIX,
	OP_POSTFIX_SEQ,
	OP_PREFIX_SEQ,

	PUNCT_COMMA,
	PUNCT_DOLLAR,
	PUNCT_LBRACE,
	PUNCT_LBRACKET,
	PUNCT_LPAREN,
	PUNCT_RBRACE,
	PUNCT_RBRACKET,
	PUNCT_RPAREN,
	PUNCT_SCOLON,

	TYPE_BOOL,
	TYPE_BYTE,
	TYPE_CONST,
	TYPE_FLOAT32,
	TYPE_FLOAT64,
	TYPE_INT,
	TYPE_INT8,
	TYPE_INT16,
	TYPE_INT32,
	TYPE_INT64,
	TYPE_TYPEOF,
	TYPE_UINT,
	TYPE_UINT8,
	TYPE_UINT16,
	TYPE_UINT32,
	TYPE_UINT64,
	TYPE_VOID,
}
