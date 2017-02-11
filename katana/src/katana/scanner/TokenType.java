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

public enum TokenType
{
	BEGIN,
	END,

	DECL_ABI,
	DECL_DATA,
	DECL_EXPORT,
	DECL_EXTERN,
	DECL_FN,
	DECL_GLOBAL,
	DECL_IMPORT,
	DECL_INFIX,
	DECL_MODULE,
	DECL_OP,
	DECL_POSTFIX,
	DECL_PREFIX,
	DECL_TYPE,

	IDENT,

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

	MISC_ALIGNOF,
	MISC_BUILTIN,
	MISC_INLINE,
	MISC_NARROW_CAST,
	MISC_OFFSETOF,
	MISC_POINTER_CAST,
	MISC_SIGN_CAST,
	MISC_SIZEOF,
	MISC_UNDEF,
	MISC_WIDEN_CAST,

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

	STMT_ELSE,
	STMT_GOTO,
	STMT_IF,
	STMT_LABEL,
	STMT_LOCAL,
	STMT_LOOP,
	STMT_RETURN,
	STMT_UNLESS,
	STMT_UNTIL,
	STMT_WHILE,

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
	TYPE_OPAQUE,
	TYPE_TYPEOF,
	TYPE_UINT,
	TYPE_UINT8,
	TYPE_UINT16,
	TYPE_UINT32,
	TYPE_UINT64,
	TYPE_VOID,
}
