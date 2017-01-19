// Copyright 2016-2017 Markus Grech
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

public class Token
{
	public enum Category
	{
		IDENT,
		OP,
		LIT,
		DECL,
		STMT,
		PUNCT,
		TYPE,
		MISC,

		BEGIN,
		END,
	}

	public enum Type
	{
		IDENT,

		OPSEQ_PREFIX,
		OPSEQ_POSTFIX,
		OP_INFIX,

		LIT_NULL,
		LIT_BOOL,
		LIT_INT8,
		LIT_INT16,
		LIT_INT32,
		LIT_INT64,
		LIT_UINT8,
		LIT_UINT16,
		LIT_UINT32,
		LIT_UINT64,
		LIT_INT,
		LIT_UINT,
		LIT_PINT,
		LIT_UPINT,
		LIT_FLOAT32,
		LIT_FLOAT64,
		LIT_STRING,
		LIT_INT_DEDUCE,
		LIT_FLOAT_DEDUCE,

		DECL_EXPORT,
		DECL_IMPORT,
		DECL_MODULE,
		DECL_GLOBAL,
		DECL_EXTERN,
		DECL_FN,
		DECL_DATA,
		DECL_TYPE,
		DECL_OP,
		DECL_PREFIX,
		DECL_INFIX,
		DECL_POSTFIX,
		DECL_ABI,

		STMT_LOCAL,
		STMT_IF,
		STMT_UNLESS,
		STMT_ELSE,
		STMT_GOTO,
		STMT_RETURN,
		STMT_LABEL,
		STMT_LOOP,
		STMT_WHILE,
		STMT_UNTIL,

		PUNCT_LPAREN,
		PUNCT_RPAREN,
		PUNCT_LBRACKET,
		PUNCT_RBRACKET,
		PUNCT_LBRACE,
		PUNCT_RBRACE,
		PUNCT_COMMA,
		PUNCT_SCOLON,
		PUNCT_DOLOLOLLAR,

		TYPE_VOID,
		TYPE_BOOL,
		TYPE_INT8,
		TYPE_INT16,
		TYPE_INT32,
		TYPE_INT64,
		TYPE_INT,
		TYPE_PINT,
		TYPE_UINT8,
		TYPE_UINT16,
		TYPE_UINT32,
		TYPE_UINT64,
		TYPE_UINT,
		TYPE_UPINT,
		TYPE_FLOAT32,
		TYPE_FLOAT64,
		TYPE_OPAQUE,
		TYPE_CONST,
		TYPE_TYPEOF,

		MISC_SIZEOF,
		MISC_ALIGNOF,
		MISC_OFFSETOF,
		MISC_INLINE,
		MISC_BUILTIN,
		MISC_UNDEF,
		MISC_NARROW_CAST,
		MISC_WIDEN_CAST,
		MISC_SIGN_CAST,
		MISC_POINTEGER_CAST,

		BEGIN,
		END,
	}

	public static final Token LIT_NULL   = new Token(Category.LIT, Type.LIT_NULL, "null");
	public static final Token LIT_BOOL_T = new Token(Category.LIT, Type.LIT_BOOL, "true",  true);
	public static final Token LIT_BOOL_F = new Token(Category.LIT, Type.LIT_BOOL, "false", false);

	public static final Token DECL_EXPORT  = new Token(Category.DECL, Type.DECL_EXPORT,  "export");
	public static final Token DECL_IMPORT  = new Token(Category.DECL, Type.DECL_IMPORT,  "import");
	public static final Token DECL_MODULE  = new Token(Category.DECL, Type.DECL_MODULE,  "module");
	public static final Token DECL_GLOBAL  = new Token(Category.DECL, Type.DECL_GLOBAL,  "global");
	public static final Token DECL_EXTERN  = new Token(Category.DECL, Type.DECL_EXTERN,  "extern");
	public static final Token DECL_FN      = new Token(Category.DECL, Type.DECL_FN,      "fn");
	public static final Token DECL_DATA    = new Token(Category.DECL, Type.DECL_DATA,    "data");
	public static final Token DECL_TYPE    = new Token(Category.DECL, Type.DECL_TYPE,    "type");
	public static final Token DECL_OP      = new Token(Category.DECL, Type.DECL_OP,      "operator");
	public static final Token DECL_PREFIX  = new Token(Category.DECL, Type.DECL_PREFIX,  "prefix");
	public static final Token DECL_INFIX   = new Token(Category.DECL, Type.DECL_INFIX,   "infix");
	public static final Token DECL_POSTFIX = new Token(Category.DECL, Type.DECL_POSTFIX, "postfix");
	public static final Token DECL_ABI     = new Token(Category.DECL, Type.DECL_ABI,     "abi");

	public static final Token STMT_LOCAL  = new Token(Category.STMT, Type.STMT_LOCAL,  "local");
	public static final Token STMT_IF     = new Token(Category.STMT, Type.STMT_IF,     "if");
	public static final Token STMT_UNLESS = new Token(Category.STMT, Type.STMT_UNLESS, "unless");
	public static final Token STMT_ELSE   = new Token(Category.STMT, Type.STMT_ELSE,   "else");
	public static final Token STMT_GOTO   = new Token(Category.STMT, Type.STMT_GOTO,   "goto");
	public static final Token STMT_RETURN = new Token(Category.STMT, Type.STMT_RETURN, "return");
	public static final Token STMT_LOOP   = new Token(Category.STMT, Type.STMT_LOOP,   "loop");
	public static final Token STMT_WHILE  = new Token(Category.STMT, Type.STMT_WHILE,  "while");
	public static final Token STMT_UNTIL  = new Token(Category.STMT, Type.STMT_UNTIL,  "until");

	public static final Token PUNCT_LPAREN     = new Token(Category.PUNCT, Type.PUNCT_LPAREN,     "(");
	public static final Token PUNCT_RPAREN     = new Token(Category.PUNCT, Type.PUNCT_RPAREN,     ")");
	public static final Token PUNCT_LBRACKET   = new Token(Category.PUNCT, Type.PUNCT_LBRACKET,   "[");
	public static final Token PUNCT_RBRACKET   = new Token(Category.PUNCT, Type.PUNCT_RBRACKET,   "]");
	public static final Token PUNCT_LBRACE     = new Token(Category.PUNCT, Type.PUNCT_LBRACE,     "{");
	public static final Token PUNCT_RBRACE     = new Token(Category.PUNCT, Type.PUNCT_RBRACE,     "}");
	public static final Token PUNCT_COMMA      = new Token(Category.PUNCT, Type.PUNCT_COMMA,      ",");
	public static final Token PUNCT_SCOLON     = new Token(Category.PUNCT, Type.PUNCT_SCOLON,     ";");
	public static final Token PUNCT_DOLOLOLLAR = new Token(Category.PUNCT, Type.PUNCT_DOLOLOLLAR, "$");

	public static final Token TYPE_VOID    = new Token(Category.TYPE, Type.TYPE_VOID,    "void");
	public static final Token TYPE_BOOL    = new Token(Category.TYPE, Type.TYPE_BOOL,    "bool");
	public static final Token TYPE_INT8    = new Token(Category.TYPE, Type.TYPE_INT8,    "int8");
	public static final Token TYPE_INT16   = new Token(Category.TYPE, Type.TYPE_INT16,   "int16");
	public static final Token TYPE_INT32   = new Token(Category.TYPE, Type.TYPE_INT32,   "int32");
	public static final Token TYPE_INT64   = new Token(Category.TYPE, Type.TYPE_INT64,   "int64");
	public static final Token TYPE_INT     = new Token(Category.TYPE, Type.TYPE_INT,     "int");
	public static final Token TYPE_PINT    = new Token(Category.TYPE, Type.TYPE_PINT,    "pint");
	public static final Token TYPE_UINT8   = new Token(Category.TYPE, Type.TYPE_UINT8,   "uint8");
	public static final Token TYPE_UINT16  = new Token(Category.TYPE, Type.TYPE_UINT16,  "uint16");
	public static final Token TYPE_UINT32  = new Token(Category.TYPE, Type.TYPE_UINT32,  "uint32");
	public static final Token TYPE_UINT64  = new Token(Category.TYPE, Type.TYPE_UINT64,  "uint64");
	public static final Token TYPE_UINT    = new Token(Category.TYPE, Type.TYPE_UINT,    "uint");
	public static final Token TYPE_UPINT   = new Token(Category.TYPE, Type.TYPE_UPINT,   "upint");
	public static final Token TYPE_FLOAT32 = new Token(Category.TYPE, Type.TYPE_FLOAT32, "float32");
	public static final Token TYPE_FLOAT64 = new Token(Category.TYPE, Type.TYPE_FLOAT64, "float64");
	public static final Token TYPE_OPAQUE  = new Token(Category.TYPE, Type.TYPE_OPAQUE,  "opaque");
	public static final Token TYPE_CONST   = new Token(Category.TYPE, Type.TYPE_CONST,   "const");
	public static final Token TYPE_TYPEOF  = new Token(Category.TYPE, Type.TYPE_TYPEOF,  "typeof");

	public static final Token MISC_SIZEOF         = new Token(Category.MISC, Type.MISC_SIZEOF,         "sizeof");
	public static final Token MISC_ALIGNOF        = new Token(Category.MISC, Type.MISC_ALIGNOF,        "alignof");
	public static final Token MISC_OFFSETOF       = new Token(Category.MISC, Type.MISC_OFFSETOF,       "offsetof");
	public static final Token MISC_INLINE         = new Token(Category.MISC, Type.MISC_INLINE,         "inline");
	public static final Token MISC_BUILTIN        = new Token(Category.MISC, Type.MISC_BUILTIN,        "builtin");
	public static final Token MISC_UNDEF          = new Token(Category.MISC, Type.MISC_UNDEF,          "undef");
	public static final Token MISC_NARROW_CAST    = new Token(Category.MISC, Type.MISC_NARROW_CAST,    "narrow_cast");
	public static final Token MISC_WIDEN_CAST     = new Token(Category.MISC, Type.MISC_WIDEN_CAST,     "widen_cast");
	public static final Token MISC_SIGN_CAST      = new Token(Category.MISC, Type.MISC_SIGN_CAST,      "sign_cast");
	public static final Token MISC_POINTEGER_CAST = new Token(Category.MISC, Type.MISC_POINTEGER_CAST, "pointeger_cast");

	public static final Token BEGIN = new Token(Category.BEGIN, Type.BEGIN, null);
	public static final Token END   = new Token(Category.END,   Type.END,   null);

	public final Category category;
	public final Type type;
	public final String value;
	public final Object data;

	public static Token identifier(String value)
	{
		return new Token(Category.IDENT, Type.IDENT, value);
	}

	public static Token op(String value, Type type)
	{
		return new Token(Category.OP, type, value);
	}

	public static Token numericLiteral(Type type, String value, int base)
	{
		return new Token(Category.LIT, type, value, base);
	}

	public static Token stringLiteral(String value)
	{
		return new Token(Category.LIT, Type.LIT_STRING, value);
	}

	public static Token label(String value)
	{
		return new Token(Category.STMT, Type.STMT_LABEL, value);
	}

	@Override
	public String toString()
	{
		return String.format("Token(%s, %s, %s)", category, type, value);
	}

	private Token(Category category, Type type, String value)
	{
		this(category, type, value, null);
	}

	private Token(Category category, Type type, String value, Object data)
	{
		this.category = category;
		this.type = type;
		this.value = value;
		this.data = data;
	}
}
