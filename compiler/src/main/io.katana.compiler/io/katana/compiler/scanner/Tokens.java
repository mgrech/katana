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

public class Tokens
{
	public static final Token KW_ABI          = new Token(TokenCategory.KW, TokenType.KW_ABI);
	public static final Token KW_ALIGNOF      = new Token(TokenCategory.KW, TokenType.KW_ALIGNOF);
	public static final Token KW_BUILTIN      = new Token(TokenCategory.KW, TokenType.KW_BUILTIN);
	public static final Token KW_DATA         = new Token(TokenCategory.KW, TokenType.KW_DATA);
	public static final Token KW_ELSE         = new Token(TokenCategory.KW, TokenType.KW_ELSE);
	public static final Token KW_EXPORT       = new Token(TokenCategory.KW, TokenType.KW_EXPORT);
	public static final Token KW_EXTERN       = new Token(TokenCategory.KW, TokenType.KW_EXTERN);
	public static final Token KW_FN           = new Token(TokenCategory.KW, TokenType.KW_FN);
	public static final Token KW_GLOBAL       = new Token(TokenCategory.KW, TokenType.KW_GLOBAL);
	public static final Token KW_GOTO         = new Token(TokenCategory.KW, TokenType.KW_GOTO);
	public static final Token KW_IF           = new Token(TokenCategory.KW, TokenType.KW_IF);
	public static final Token KW_IMPORT       = new Token(TokenCategory.KW, TokenType.KW_IMPORT);
	public static final Token KW_INFIX        = new Token(TokenCategory.KW, TokenType.KW_INFIX);
	public static final Token KW_INLINE       = new Token(TokenCategory.KW, TokenType.KW_INLINE);
	public static final Token KW_LOOP         = new Token(TokenCategory.KW, TokenType.KW_LOOP);
	public static final Token KW_MODULE       = new Token(TokenCategory.KW, TokenType.KW_MODULE);
	public static final Token KW_NARROW_CAST  = new Token(TokenCategory.KW, TokenType.KW_NARROW_CAST);
	public static final Token KW_OFFSETOF     = new Token(TokenCategory.KW, TokenType.KW_OFFSETOF);
	public static final Token KW_OPERATOR     = new Token(TokenCategory.KW, TokenType.KW_OPERATOR);
	public static final Token KW_OPAQUE       = new Token(TokenCategory.KW, TokenType.KW_OPAQUE);
	public static final Token KW_POINTER_CAST = new Token(TokenCategory.KW, TokenType.KW_POINTER_CAST);
	public static final Token KW_POSTFIX      = new Token(TokenCategory.KW, TokenType.KW_POSTFIX);
	public static final Token KW_PREFIX       = new Token(TokenCategory.KW, TokenType.KW_PREFIX);
	public static final Token KW_RETURN       = new Token(TokenCategory.KW, TokenType.KW_RETURN);
	public static final Token KW_SIGN_CAST    = new Token(TokenCategory.KW, TokenType.KW_SIGN_CAST);
	public static final Token KW_SIZEOF       = new Token(TokenCategory.KW, TokenType.KW_SIZEOF);
	public static final Token KW_TYPE         = new Token(TokenCategory.KW, TokenType.KW_TYPE);
	public static final Token KW_UNDEF        = new Token(TokenCategory.KW, TokenType.KW_UNDEF);
	public static final Token KW_UNLESS       = new Token(TokenCategory.KW, TokenType.KW_UNLESS);
	public static final Token KW_UNREACHABLE  = new Token(TokenCategory.KW, TokenType.KW_UNREACHABLE);
	public static final Token KW_UNTIL        = new Token(TokenCategory.KW, TokenType.KW_UNTIL);
	public static final Token KW_VAR          = new Token(TokenCategory.KW, TokenType.KW_VAR);
	public static final Token KW_WHILE        = new Token(TokenCategory.KW, TokenType.KW_WHILE);
	public static final Token KW_WIDEN_CAST   = new Token(TokenCategory.KW, TokenType.KW_WIDEN_CAST);

	public static final Token LIT_BOOL_F = new Token(TokenCategory.LIT, TokenType.LIT_BOOL, false);
	public static final Token LIT_BOOL_T = new Token(TokenCategory.LIT, TokenType.LIT_BOOL, true);
	public static final Token LIT_NULL   = new Token(TokenCategory.LIT, TokenType.LIT_NULL);

	public static final Token PUNCT_COMMA    = new Token(TokenCategory.PUNCT, TokenType.PUNCT_COMMA);
	public static final Token PUNCT_DOLLAR   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_DOLLAR);
	public static final Token PUNCT_LBRACE   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_LBRACE);
	public static final Token PUNCT_LBRACKET = new Token(TokenCategory.PUNCT, TokenType.PUNCT_LBRACKET);
	public static final Token PUNCT_LPAREN   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_LPAREN);
	public static final Token PUNCT_RBRACE   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_RBRACE);
	public static final Token PUNCT_RBRACKET = new Token(TokenCategory.PUNCT, TokenType.PUNCT_RBRACKET);
	public static final Token PUNCT_RPAREN   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_RPAREN);
	public static final Token PUNCT_SCOLON   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_SCOLON);

	public static final Token TYPE_BOOL    = new Token(TokenCategory.TYPE, TokenType.TYPE_BOOL);
	public static final Token TYPE_BYTE    = new Token(TokenCategory.TYPE, TokenType.TYPE_BYTE);
	public static final Token TYPE_CONST   = new Token(TokenCategory.TYPE, TokenType.TYPE_CONST);
	public static final Token TYPE_FLOAT32 = new Token(TokenCategory.TYPE, TokenType.TYPE_FLOAT32);
	public static final Token TYPE_FLOAT64 = new Token(TokenCategory.TYPE, TokenType.TYPE_FLOAT64);
	public static final Token TYPE_INT     = new Token(TokenCategory.TYPE, TokenType.TYPE_INT);
	public static final Token TYPE_INT8    = new Token(TokenCategory.TYPE, TokenType.TYPE_INT8);
	public static final Token TYPE_INT16   = new Token(TokenCategory.TYPE, TokenType.TYPE_INT16);
	public static final Token TYPE_INT32   = new Token(TokenCategory.TYPE, TokenType.TYPE_INT32);
	public static final Token TYPE_INT64   = new Token(TokenCategory.TYPE, TokenType.TYPE_INT64);
	public static final Token TYPE_TYPEOF  = new Token(TokenCategory.TYPE, TokenType.TYPE_TYPEOF);
	public static final Token TYPE_UINT    = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT);
	public static final Token TYPE_UINT8   = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT8);
	public static final Token TYPE_UINT16  = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT16);
	public static final Token TYPE_UINT32  = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT32);
	public static final Token TYPE_UINT64  = new Token(TokenCategory.TYPE, TokenType.TYPE_UINT64);
	public static final Token TYPE_VOID    = new Token(TokenCategory.TYPE, TokenType.TYPE_VOID);

	public static Token identifier(String value)
	{
		return new Token(TokenCategory.IDENT, TokenType.IDENT, value);
	}

	public static Token label(String value)
	{
		return new Token(TokenCategory.LABEL, TokenType.LABEL, value);
	}

	public static Token numericLiteral(TokenType type, Object value)
	{
		return new Token(TokenCategory.LIT, type, value);
	}

	public static Token op(String value, TokenType type)
	{
		return new Token(TokenCategory.OP, type, value);
	}

	public static Token stringLiteral(String value)
	{
		return new Token(TokenCategory.LIT, TokenType.LIT_STRING, value);
	}
}
