// Copyright 2016 Markus Grech
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

public class Scanner
{
	public Scanner(int[] source)
	{
		this.source = source;
	}

	public int line() { return line; }
	public int column() { return tokenColumn; }
	public int offset() { return tokenOffset; }

	public Token token() { return token; }
	public void advance() { token = doAdvance(); }

	private Token doAdvance()
	{
		skipWhitespaceAndComments();

		if(atEnd())
			return Token.END;

		tokenOffset = currentOffset;
		tokenColumn = currentColumn;

		int cp = here();

		switch(cp)
		{
		case '(': advanceColumn(); return Token.PUNCT_LPAREN;
		case ')': advanceColumn(); return Token.PUNCT_RPAREN;
		case '[': advanceColumn(); return Token.PUNCT_LBRACKET;
		case ']': advanceColumn(); return Token.PUNCT_RBRACKET;
		case '{': advanceColumn(); return Token.PUNCT_LBRACE;
		case '}': advanceColumn(); return Token.PUNCT_RBRACE;
		case '.': advanceColumn(); return Token.PUNCT_DOT;
		case ',': advanceColumn(); return Token.PUNCT_COMMA;
		case ':': advanceColumn(); return Token.PUNCT_COLON;
		case ';': advanceColumn(); return Token.PUNCT_SCOLON;
		case '?': advanceColumn(); return Token.PUNCT_QMARK;

		case '=':
			advanceColumn();

			if(!atEnd() && here() == '>')
			{
				advanceColumn();
				return Token.PUNCT_RET;
			}

			return Token.PUNCT_ASSIGN;

		case '@': advanceColumn(); return label();
		case '"': advanceColumn(); return stringLiteral();

		case '!': advanceColumn(); return Token.STMT_NEGATE;

		default: break;
		}

		if(cp == '-' || CharClassifier.isDigit(cp))
			return numericLiteral();

		if(CharClassifier.isIdentifierStart(cp))
			return identifierOrKeyword();

		error("invalid character encountered: " + formatCodepoint(cp));
		throw new AssertionError("unreachable");
	}

	private Token label()
	{
		StringBuilder builder = new StringBuilder();

		while(!atEnd() && CharClassifier.isIdentifierChar(here()))
		{
			builder.appendCodePoint(here());
			advanceColumn();
		}

		return Token.label(builder.toString());
	}

	private String formatCodepoint(int cp)
	{
		if(cp <= 0xFFFF)
			return String.format("U+%04X", cp);

		return String.format("U+%X", cp);
	}

	private Token stringLiteral()
	{
		int line = line();

		StringBuilder builder = new StringBuilder();

		while(!atEnd() && here() != '"')
			builder.appendCodePoint(stringCodepoint());

		if(atEnd())
			throw new RuntimeException("unterminated string literal on line " + line);

		advanceColumn();

		return Token.literal(Token.Type.LIT_STRING, builder.toString());
	}

	private int stringCodepoint()
	{
		if(here() == '\\')
		{
			advanceColumn();

			int escape = here();
			advanceColumn();

			switch(escape)
			{
			case '0': return 0;    // null
			case 'a': return 0x07; // audible bell
			case 'b': return '\b'; // backspace
			case 'e': return 0x1B; // escape
			case 'f': return '\f'; // form feed
			case 'n': return '\n'; // line feed
			case 'r': return '\r'; // carriage return
			case 't': return '\t'; // horizontal tabulation
			case 'v': return 0x0B; // vertical tabulation

			case 'U': return unicodeEscape(true);
			case 'u': return unicodeEscape(false);
			case 'x': return hexEscape();

			case '"': return '"';
			case '\\': return '\\';

			default: error("invalid escape sequence \\" + formatCodepoint(here()));
			}
		}

		int cp = here();
		advanceColumn();
		return cp;
	}

	private int unicodeEscape(boolean big)
	{
		int d1 = (big ? hexDigit() : 0) << 20;
		int d2 = (big ? hexDigit() : 0) << 16;
		int d3 = hexDigit() << 12;
		int d4 = hexDigit() <<  8;
		int d5 = hexDigit() <<  4;
		int d6 = hexDigit();
		int sum =  d1 | d2 | d3 | d4 | d5 | d6;

		if(sum > 0x10FFFF)
			error("invalid codepoint in unicode escape sequence");

		return sum;
	}

	private int hexEscape()
	{
		int d1 = hexDigit();
		int d2 = hexDigit();
		return 16 * d1 + d2;
	}

	private int hexDigit()
	{
		if(atEnd() || !CharClassifier.isHexDigit(here()))
			error("expected hex digit in escape sequence");

		int digit = fromHexDigit(here());
		advanceColumn();
		return digit;
	}

	private int fromHexDigit(int cp)
	{
		if(CharClassifier.isDigit(cp))
			return cp - '0';

		if(cp >= 'a' && cp <= 'f')
			return 10 + cp - 'a';

		if(cp >= 'A' && cp <= 'F')
			return 10 + cp - 'A';

		throw new RuntimeException("invalid argument");
	}

	private Token identifierOrKeyword()
	{
		StringBuilder builder = new StringBuilder();

		do
		{
			builder.appendCodePoint(here());
			advanceColumn();
		}
		while(!atEnd() && CharClassifier.isIdentifierChar(here()));

		return checkForKeywords(builder.toString());
	}

	private Token checkForKeywords(String value)
	{
		switch(value)
		{
		case "null":  return Token.LIT_NULL;
		case "true":  return Token.LIT_BOOL_T;
		case "false": return Token.LIT_BOOL_F;

		case "export": return Token.DECL_EXPORT;
		case "import": return Token.DECL_IMPORT;
		case "module": return Token.DECL_MODULE;
		case "global": return Token.DECL_GLOBAL;
		case "extern": return Token.DECL_EXTERN;
		case "fn":     return Token.DECL_FN;
		case "data":   return Token.DECL_DATA;

		case "if":     return Token.STMT_IF;
		case "else":   return Token.STMT_ELSE;
		case "goto":   return Token.STMT_GOTO;
		case "return": return Token.STMT_RETURN;
		case "loop":   return Token.STMT_LOOP;
		case "while":  return Token.STMT_WHILE;

		case "bool":    return Token.TYPE_BOOL;
		case "int8":    return Token.TYPE_INT8;
		case "int16":   return Token.TYPE_INT16;
		case "int32":   return Token.TYPE_INT32;
		case "int64":   return Token.TYPE_INT64;
		case "int":     return Token.TYPE_INT;
		case "pint":    return Token.TYPE_PINT;
		case "uint8":   return Token.TYPE_UINT8;
		case "uint16":  return Token.TYPE_UINT16;
		case "uint32":  return Token.TYPE_UINT32;
		case "uint64":  return Token.TYPE_UINT64;
		case "uint":    return Token.TYPE_UINT;
		case "upint":   return Token.TYPE_UPINT;
		case "float32": return Token.TYPE_FLOAT32;
		case "float64": return Token.TYPE_FLOAT64;
		case "ptr":     return Token.TYPE_PTR;
		case "opaque":  return Token.TYPE_OPAQUE;
		case "typeof":  return Token.TYPE_TYPEOF;

		case "sizeof":    return Token.MISC_SIZEOF;
		case "alignof":   return Token.MISC_ALIGNOF;
		case "offsetof":  return Token.MISC_OFFSETOF;
		case "inline":    return Token.MISC_INLINE;
		case "addressof": return Token.MISC_ADDRESSOF;
		case "deref":     return Token.MISC_DEREF;
		case "builtin":   return Token.MISC_BUILTIN;

		default: return Token.identifier(value);
		}
	}

	private Token numericLiteral()
	{
		StringBuilder literal = new StringBuilder();

		boolean isNegativeLiteral = here() == '-';

		do
		{
			literal.appendCodePoint(here());
			advanceColumn();
		}
		while(!atEnd() && CharClassifier.isDigit(here()));

		boolean isFloatingPointLiteral = !atEnd() && here() == '.';

		if(isFloatingPointLiteral)
		{
			literal.append('.');
			advanceColumn();

			while(!atEnd() && CharClassifier.isDigit(here()))
			{
				literal.appendCodePoint(here());
				advanceColumn();
			}
		}

		StringBuilder suffix = new StringBuilder();

		while(!atEnd() && (CharClassifier.isIdentifierChar(here()) || CharClassifier.isDigit(here())))
		{
			suffix.appendCodePoint(here());
			advanceColumn();
		}

		Token.Type type = null;

		boolean isFloatingPointSuffix = false;
		boolean isUnsignedSuffix = false;

		switch(suffix.toString())
		{
		case "i":   type = Token.Type.LIT_INT;   break;
		case "pi":  type = Token.Type.LIT_PINT;  break;
		case "i8":  type = Token.Type.LIT_INT8;  break;
		case "i16": type = Token.Type.LIT_INT16; break;
		case "i32": type = Token.Type.LIT_INT32; break;
		case "i64": type = Token.Type.LIT_INT64; break;

		case "u":   type = Token.Type.LIT_UINT;   isUnsignedSuffix = true; break;
		case "pu":  type = Token.Type.LIT_UPINT;  isUnsignedSuffix = true; break;
		case "u8":  type = Token.Type.LIT_UINT8;  isUnsignedSuffix = true; break;
		case "u16": type = Token.Type.LIT_UINT16; isUnsignedSuffix = true; break;
		case "u32": type = Token.Type.LIT_UINT32; isUnsignedSuffix = true; break;
		case "u64": type = Token.Type.LIT_UINT64; isUnsignedSuffix = true; break;

		case "f32": type = Token.Type.LIT_FLOAT32; isFloatingPointSuffix = true; break;
		case "f64": type = Token.Type.LIT_FLOAT64; isFloatingPointSuffix = true; break;

		case "": error("numeric literal without suffix");
		default: error("unknown literal suffix '" + suffix + "'");
		}

		if(isFloatingPointLiteral && !isFloatingPointSuffix)
			error("integer suffix used on floating point literal");

		if(isNegativeLiteral && isUnsignedSuffix)
			error("unsigned integer suffix used on signed number literal");

		return Token.literal(type, literal.toString());
	}

	private void skipWhitespaceAndComments()
	{
		for(;;)
		{
			skipWhitespace();

			if(atComment())
				skipComment();

			if(!atLineBreak())
				break;

			skipLineBreak();
		}
	}

	private void skipWhitespace()
	{
		while(!atEnd() && CharClassifier.isWhitespace(here()))
			advanceColumn();
	}

	private void skipComment()
	{
		do advanceColumn();
		while(!atEnd() && !atLineBreak());
	}

	private boolean atLineBreak()
	{
		return !atEnd() && CharClassifier.isLineBreak(here());
	}

	private boolean atComment()
	{
		return !atEnd() && here() == '#';
	}

	private boolean atEnd()
	{
		return currentOffset == source.length;
	}

	private int here()
	{
		return source[currentOffset];
	}

	private void skipLineBreak()
	{
		++currentOffset;
		++line;
		currentColumn = 1;
	}

	private void advanceColumn()
	{
		++currentOffset;
		++currentColumn;
	}

	private void error(String message)
	{
		throw new RuntimeException(message + " on line " + line());
	}

	private int[] source;
	private int line = 1;
	private int currentOffset = 0;
	private int tokenOffset = 0;
	private int currentColumn = 1;
	private int tokenColumn = 1;

	private Token token = Token.BEGIN;
}
