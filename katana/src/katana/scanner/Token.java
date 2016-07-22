package katana.scanner;

public class Token
{
	public enum Category
	{
		IDENT,
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

		LIT_UNINIT,
		LIT_NULL,
		LIT_BOOL,
		LIT_INT,
		LIT_FLOAT,
		LIT_STRING,

		DECL_EXPORT,
		DECL_IMPORT,
		DECL_MODULE,
		DECL_VAR,
		DECL_FN,
		DECL_DATA,

		STMT_IF,
		STMT_ELSE,
		STMT_GOTO,
		STMT_RETURN,
		STMT_LABEL,

		PUNCT_LPAREN,
		PUNCT_RPAREN,
		PUNCT_LBRACKET,
		PUNCT_RBRACKET,
		PUNCT_LBRACE,
		PUNCT_RBRACE,
		PUNCT_DOT,
		PUNCT_COMMA,
		PUNCT_COLON,
		PUNCT_SCOLON,
		PUNCT_QMARK,
		PUNCT_ASSIGN,
		PUNCT_RET,

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
		TYPE_PTR,

		MISC_SIZEOF,
		MISC_ALIGNOF,
		MISC_OFFSETOF,
		MISC_INTRINSIC,
		MISC_INLINE,
		MISC_ADDRESSOF,
		MISC_DEREFERENCE,

		BEGIN,
		END,
	}

	public static final Token LIT_UNINIT = new Token(Category.LIT, Type.LIT_UNINIT, "uninitialized");
	public static final Token LIT_NULL   = new Token(Category.LIT, Type.LIT_NULL,   "null");
	public static final Token LIT_BOOL_T = new Token(Category.LIT, Type.LIT_BOOL,   "true");
	public static final Token LIT_BOOL_F = new Token(Category.LIT, Type.LIT_BOOL,   "false");

	public static final Token DECL_EXPORT = new Token(Category.DECL, Type.DECL_EXPORT, "export");
	public static final Token DECL_IMPORT = new Token(Category.DECL, Type.DECL_IMPORT, "import");
	public static final Token DECL_MODULE = new Token(Category.DECL, Type.DECL_MODULE, "module");
	public static final Token DECL_VAR    = new Token(Category.DECL, Type.DECL_VAR,    "var");
	public static final Token DECL_FN     = new Token(Category.DECL, Type.DECL_FN,     "fn");
	public static final Token DECL_DATA   = new Token(Category.DECL, Type.DECL_DATA,   "data");

	public static final Token STMT_IF     = new Token(Category.STMT, Type.STMT_IF,     "if");
	public static final Token STMT_ELSE   = new Token(Category.STMT, Type.STMT_ELSE,   "else");
	public static final Token STMT_GOTO   = new Token(Category.STMT, Type.STMT_GOTO,   "goto");
	public static final Token STMT_RETURN = new Token(Category.STMT, Type.STMT_RETURN, "return");

	public static final Token PUNCT_LPAREN   = new Token(Category.PUNCT, Type.PUNCT_LPAREN,   "(");
	public static final Token PUNCT_RPAREN   = new Token(Category.PUNCT, Type.PUNCT_RPAREN,   ")");
	public static final Token PUNCT_LBRACKET = new Token(Category.PUNCT, Type.PUNCT_LBRACKET, "[");
	public static final Token PUNCT_RBRACKET = new Token(Category.PUNCT, Type.PUNCT_RBRACKET, "]");
	public static final Token PUNCT_LBRACE   = new Token(Category.PUNCT, Type.PUNCT_LBRACE,   "{");
	public static final Token PUNCT_RBRACE   = new Token(Category.PUNCT, Type.PUNCT_RBRACE,   "}");
	public static final Token PUNCT_DOT      = new Token(Category.PUNCT, Type.PUNCT_DOT,      ".");
	public static final Token PUNCT_COMMA    = new Token(Category.PUNCT, Type.PUNCT_COMMA,    ",");
	public static final Token PUNCT_COLON    = new Token(Category.PUNCT, Type.PUNCT_COLON,    ":");
	public static final Token PUNCT_SCOLON   = new Token(Category.PUNCT, Type.PUNCT_SCOLON,   ";");
	public static final Token PUNCT_QMARK    = new Token(Category.PUNCT, Type.PUNCT_QMARK,    "?");
	public static final Token PUNCT_ASSIGN   = new Token(Category.PUNCT, Type.PUNCT_ASSIGN,   "=");
	public static final Token PUNCT_RET      = new Token(Category.PUNCT, Type.PUNCT_RET,      "=>");

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
	public static final Token TYPE_PTR     = new Token(Category.TYPE, Type.TYPE_PTR,     "*");

	public static final Token MISC_SIZEOF      = new Token(Category.MISC, Type.MISC_SIZEOF,    "sizeof");
	public static final Token MISC_ALIGNOF     = new Token(Category.MISC, Type.MISC_ALIGNOF,   "alignof");
	public static final Token MISC_OFFSETOF    = new Token(Category.MISC, Type.MISC_OFFSETOF,  "offsetof");
	public static final Token MISC_INTRINSIC   = new Token(Category.MISC, Type.MISC_INTRINSIC, "intrinsic");
	public static final Token MISC_INLINE      = new Token(Category.MISC, Type.MISC_INLINE,    "inline");
	public static final Token MISC_ADDRESSOF   = new Token(Category.MISC, Type.MISC_ADDRESSOF, "addressof");
	public static final Token MISC_DEREFERENCE = new Token(Category.MISC, Type.MISC_DEREFERENCE, "dereference");

	public static final Token BEGIN = new Token(Category.BEGIN, Type.BEGIN, null);
	public static final Token END   = new Token(Category.END,   Type.END,   null);

	public final Category category;
	public final Type type;
	public final String value;

	public static Token identifier(String value)
	{
		return new Token(Category.IDENT, Type.IDENT, value);
	}

	public static Token literal(Type type, String value)
	{
		return new Token(Category.LIT, type, value);
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
		this.category = category;
		this.type = type;
		this.value = value;
	}
}
