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

public class Tokens
{
	public static final Token DECL_ABI     = new Token(TokenCategory.DECL, TokenType.DECL_ABI);
	public static final Token DECL_DATA    = new Token(TokenCategory.DECL, TokenType.DECL_DATA);
	public static final Token DECL_EXPORT  = new Token(TokenCategory.DECL, TokenType.DECL_EXPORT);
	public static final Token DECL_EXTERN  = new Token(TokenCategory.DECL, TokenType.DECL_EXTERN);
	public static final Token DECL_FN      = new Token(TokenCategory.DECL, TokenType.DECL_FN);
	public static final Token DECL_GLOBAL  = new Token(TokenCategory.DECL, TokenType.DECL_GLOBAL);
	public static final Token DECL_IMPORT  = new Token(TokenCategory.DECL, TokenType.DECL_IMPORT);
	public static final Token DECL_INFIX   = new Token(TokenCategory.DECL, TokenType.DECL_INFIX);
	public static final Token DECL_MODULE  = new Token(TokenCategory.DECL, TokenType.DECL_MODULE);
	public static final Token DECL_OP      = new Token(TokenCategory.DECL, TokenType.DECL_OP);
	public static final Token DECL_OPAQUE  = new Token(TokenCategory.TYPE, TokenType.DECL_OPAQUE);
	public static final Token DECL_POSTFIX = new Token(TokenCategory.DECL, TokenType.DECL_POSTFIX);
	public static final Token DECL_PREFIX  = new Token(TokenCategory.DECL, TokenType.DECL_PREFIX);
	public static final Token DECL_TYPE    = new Token(TokenCategory.DECL, TokenType.DECL_TYPE);

	public static final Token LIT_BOOL_F = new Token(TokenCategory.LIT, TokenType.LIT_BOOL, false);
	public static final Token LIT_BOOL_T = new Token(TokenCategory.LIT, TokenType.LIT_BOOL, true);
	public static final Token LIT_NULL   = new Token(TokenCategory.LIT, TokenType.LIT_NULL);

	public static final Token MISC_ALIGNOF      = new Token(TokenCategory.MISC, TokenType.MISC_ALIGNOF);
	public static final Token MISC_BUILTIN      = new Token(TokenCategory.MISC, TokenType.MISC_BUILTIN);
	public static final Token MISC_INLINE       = new Token(TokenCategory.MISC, TokenType.MISC_INLINE);
	public static final Token MISC_NARROW_CAST  = new Token(TokenCategory.MISC, TokenType.MISC_NARROW_CAST);
	public static final Token MISC_OFFSETOF     = new Token(TokenCategory.MISC, TokenType.MISC_OFFSETOF);
	public static final Token MISC_POINTER_CAST = new Token(TokenCategory.MISC, TokenType.MISC_POINTER_CAST);
	public static final Token MISC_SIGN_CAST    = new Token(TokenCategory.MISC, TokenType.MISC_SIGN_CAST);
	public static final Token MISC_SIZEOF       = new Token(TokenCategory.MISC, TokenType.MISC_SIZEOF);
	public static final Token MISC_UNDEF        = new Token(TokenCategory.MISC, TokenType.MISC_UNDEF);
	public static final Token MISC_WIDEN_CAST   = new Token(TokenCategory.MISC, TokenType.MISC_WIDEN_CAST);

	public static final Token PUNCT_COMMA    = new Token(TokenCategory.PUNCT, TokenType.PUNCT_COMMA);
	public static final Token PUNCT_DOLLAR   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_DOLLAR);
	public static final Token PUNCT_LBRACE   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_LBRACE);
	public static final Token PUNCT_LBRACKET = new Token(TokenCategory.PUNCT, TokenType.PUNCT_LBRACKET);
	public static final Token PUNCT_LPAREN   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_LPAREN);
	public static final Token PUNCT_RBRACE   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_RBRACE);
	public static final Token PUNCT_RBRACKET = new Token(TokenCategory.PUNCT, TokenType.PUNCT_RBRACKET);
	public static final Token PUNCT_RPAREN   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_RPAREN);
	public static final Token PUNCT_SCOLON   = new Token(TokenCategory.PUNCT, TokenType.PUNCT_SCOLON);

	public static final Token STMT_ELSE   = new Token(TokenCategory.STMT, TokenType.STMT_ELSE);
	public static final Token STMT_GOTO   = new Token(TokenCategory.STMT, TokenType.STMT_GOTO);
	public static final Token STMT_IF     = new Token(TokenCategory.STMT, TokenType.STMT_IF);
	public static final Token STMT_LOCAL  = new Token(TokenCategory.STMT, TokenType.STMT_LOCAL);
	public static final Token STMT_LOOP   = new Token(TokenCategory.STMT, TokenType.STMT_LOOP);
	public static final Token STMT_RETURN = new Token(TokenCategory.STMT, TokenType.STMT_RETURN);
	public static final Token STMT_UNLESS = new Token(TokenCategory.STMT, TokenType.STMT_UNLESS);
	public static final Token STMT_UNTIL  = new Token(TokenCategory.STMT, TokenType.STMT_UNTIL);
	public static final Token STMT_WHILE  = new Token(TokenCategory.STMT, TokenType.STMT_WHILE);

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
		return new Token(TokenCategory.STMT, TokenType.STMT_LABEL, value);
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
