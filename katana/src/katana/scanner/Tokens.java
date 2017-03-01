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

public class Tokens
{
	public static final Token DECL_ABI     = new Token(TokenCategory.DECL, TokenType.DECL_ABI,     "abi");
	public static final Token DECL_DATA    = new Token(TokenCategory.DECL, TokenType.DECL_DATA,    "data");
	public static final Token DECL_EXPORT  = new Token(TokenCategory.DECL, TokenType.DECL_EXPORT,  "export");
	public static final Token DECL_EXTERN  = new Token(TokenCategory.DECL, TokenType.DECL_EXTERN,  "extern");
	public static final Token DECL_FN      = new Token(TokenCategory.DECL, TokenType.DECL_FN,      "fn");
	public static final Token DECL_GLOBAL  = new Token(TokenCategory.DECL, TokenType.DECL_GLOBAL,  "global");
	public static final Token DECL_IMPORT  = new Token(TokenCategory.DECL, TokenType.DECL_IMPORT,  "import");
	public static final Token DECL_INFIX   = new Token(TokenCategory.DECL, TokenType.DECL_INFIX,   "infix");
	public static final Token DECL_MODULE  = new Token(TokenCategory.DECL, TokenType.DECL_MODULE,  "module");
	public static final Token DECL_OP      = new Token(TokenCategory.DECL, TokenType.DECL_OP,      "operator");
	public static final Token DECL_POSTFIX = new Token(TokenCategory.DECL, TokenType.DECL_POSTFIX, "postfix");
	public static final Token DECL_PREFIX  = new Token(TokenCategory.DECL, TokenType.DECL_PREFIX,  "prefix");
	public static final Token DECL_TYPE    = new Token(TokenCategory.DECL, TokenType.DECL_TYPE,    "type");

	public static final Token LIT_BOOL_F = new Token(TokenCategory.LIT, TokenType.LIT_BOOL, -1, "false", false);
	public static final Token LIT_BOOL_T = new Token(TokenCategory.LIT, TokenType.LIT_BOOL, -1, "true",  true);
	public static final Token LIT_NULL   = new Token(TokenCategory.LIT, TokenType.LIT_NULL, "null");

	public static final Token MISC_ALIGNOF      = new Token(TokenCategory.MISC, TokenType.MISC_ALIGNOF,      "alignof");
	public static final Token MISC_BUILTIN      = new Token(TokenCategory.MISC, TokenType.MISC_BUILTIN,      "builtin");
	public static final Token MISC_INLINE       = new Token(TokenCategory.MISC, TokenType.MISC_INLINE,       "inline");
	public static final Token MISC_NARROW_CAST  = new Token(TokenCategory.MISC, TokenType.MISC_NARROW_CAST,  "narrow_cast");
	public static final Token MISC_OFFSETOF     = new Token(TokenCategory.MISC, TokenType.MISC_OFFSETOF,     "offsetof");
	public static final Token MISC_POINTER_CAST = new Token(TokenCategory.MISC, TokenType.MISC_POINTER_CAST, "pointer_cast");
	public static final Token MISC_SIGN_CAST    = new Token(TokenCategory.MISC, TokenType.MISC_SIGN_CAST,    "sign_cast");
	public static final Token MISC_SIZEOF       = new Token(TokenCategory.MISC, TokenType.MISC_SIZEOF,       "sizeof");
	public static final Token MISC_UNDEF        = new Token(TokenCategory.MISC, TokenType.MISC_UNDEF,        "undef");
	public static final Token MISC_WIDEN_CAST   = new Token(TokenCategory.MISC, TokenType.MISC_WIDEN_CAST,   "widen_cast");

	public static final Token PUNCT_COMMA    = new Token(TokenCategory.PUNCT, TokenType.PUNCT_COMMA,    ",");
	public static final Token PUNCT_DOLLAR   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_DOLLAR,   "$");
	public static final Token PUNCT_LBRACE   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_LBRACE,   "{");
	public static final Token PUNCT_LBRACKET = new Token(TokenCategory.PUNCT, TokenType.PUNCT_LBRACKET, "[");
	public static final Token PUNCT_LPAREN   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_LPAREN,   "(");
	public static final Token PUNCT_RBRACE   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_RBRACE,   "}");
	public static final Token PUNCT_RBRACKET = new Token(TokenCategory.PUNCT, TokenType.PUNCT_RBRACKET, "]");
	public static final Token PUNCT_RPAREN   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_RPAREN,   ")");
	public static final Token PUNCT_SCOLON   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_SCOLON,   ";");

	public static final Token STMT_ELSE   = new Token(TokenCategory.STMT, TokenType.STMT_ELSE,   "else");
	public static final Token STMT_GOTO   = new Token(TokenCategory.STMT, TokenType.STMT_GOTO,   "goto");
	public static final Token STMT_IF     = new Token(TokenCategory.STMT, TokenType.STMT_IF,     "if");
	public static final Token STMT_LOCAL  = new Token(TokenCategory.STMT, TokenType.STMT_LOCAL,  "local");
	public static final Token STMT_LOOP   = new Token(TokenCategory.STMT, TokenType.STMT_LOOP,   "loop");
	public static final Token STMT_RETURN = new Token(TokenCategory.STMT, TokenType.STMT_RETURN, "return");
	public static final Token STMT_UNLESS = new Token(TokenCategory.STMT, TokenType.STMT_UNLESS, "unless");
	public static final Token STMT_UNTIL  = new Token(TokenCategory.STMT, TokenType.STMT_UNTIL,  "until");
	public static final Token STMT_WHILE  = new Token(TokenCategory.STMT, TokenType.STMT_WHILE,  "while");

	public static final Token TYPE_BOOL    = new Token(TokenCategory.TYPE, TokenType.TYPE_BOOL,    "bool");
	public static final Token TYPE_BYTE    = new Token(TokenCategory.TYPE, TokenType.TYPE_BYTE,    "byte");
	public static final Token TYPE_CONST   = new Token(TokenCategory.TYPE, TokenType.TYPE_CONST,   "const");
	public static final Token TYPE_FLOAT32 = new Token(TokenCategory.TYPE, TokenType.TYPE_FLOAT32, "float32");
	public static final Token TYPE_FLOAT64 = new Token(TokenCategory.TYPE, TokenType.TYPE_FLOAT64, "float64");
	public static final Token TYPE_INT     = new Token(TokenCategory.TYPE, TokenType.TYPE_INT,     "int");
	public static final Token TYPE_INT8    = new Token(TokenCategory.TYPE, TokenType.TYPE_INT8,    "int8");
	public static final Token TYPE_INT16   = new Token(TokenCategory.TYPE, TokenType.TYPE_INT16,   "int16");
	public static final Token TYPE_INT32   = new Token(TokenCategory.TYPE, TokenType.TYPE_INT32,   "int32");
	public static final Token TYPE_INT64   = new Token(TokenCategory.TYPE, TokenType.TYPE_INT64,   "int64");
	public static final Token TYPE_OPAQUE  = new Token(TokenCategory.TYPE, TokenType.TYPE_OPAQUE,  "opaque");
	public static final Token TYPE_TYPEOF  = new Token(TokenCategory.TYPE, TokenType.TYPE_TYPEOF,  "typeof");
	public static final Token TYPE_UINT    = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT,    "uint");
	public static final Token TYPE_UINT8   = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT8,   "uint8");
	public static final Token TYPE_UINT16  = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT16,  "uint16");
	public static final Token TYPE_UINT32  = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT32,  "uint32");
	public static final Token TYPE_UINT64  = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT64,  "uint64");
	public static final Token TYPE_VOID    = new Token(TokenCategory.TYPE, TokenType.TYPE_VOID,    "void");

	public static Token identifier(String value)
	{
		return new Token(TokenCategory.IDENT, TokenType.IDENT, value);
	}

	public static Token label(String value)
	{
		return new Token(TokenCategory.STMT, TokenType.STMT_LABEL, value);
	}

	public static Token numericLiteral(TokenType type, String value, int base)
	{
		return new Token(TokenCategory.LIT, type, -1, value, base);
	}

	public static Token op(String value, TokenType type)
	{
		return new Token(TokenCategory.OP, type, value);
	}

	public static Token stringLiteral(String token, String value)
	{
		return new Token(TokenCategory.LIT, TokenType.LIT_STRING, -1, token, value);
	}
}
