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

import java.nio.file.Path;

public class Scanner
{
	public Scanner(Path path, int[] source)
	{
		this.path = path;
		this.source = source;
	}

	public Path path()
	{
		return path;
	}

	public ScannerState state()
	{
		return state;
	}

	public ScannerState capture()
	{
		return state.clone();
	}

	public void backtrack(ScannerState state)
	{
		this.state = state;
	}

	public void advance() { state.token = doAdvance(); }

	private Token doAdvance()
	{
		skipWhitespaceAndComments();

		if(atEnd())
			return Token.END;

		state.tokenOffset = state.currentOffset;
		state.tokenColumn = state.currentColumn;

		int cp = here();

		switch(cp)
		{
		case '(': advanceColumn(); return Token.PUNCT_LPAREN;
		case ')': advanceColumn(); return Token.PUNCT_RPAREN;
		case '[': advanceColumn(); return Token.PUNCT_LBRACKET;
		case ']': advanceColumn(); return Token.PUNCT_RBRACKET;
		case '{': advanceColumn(); return Token.PUNCT_LBRACE;
		case '}': advanceColumn(); return Token.PUNCT_RBRACE;
		case ',': advanceColumn(); return Token.PUNCT_COMMA;
		case ';': advanceColumn(); return Token.PUNCT_SCOLON;
		case '@': advanceColumn(); return label();
		case '"': advanceColumn(); return stringLiteral();

		default: break;
		}

		if(CharClassifier.isOpChar(cp))
			return operatorSeq();

		if(CharClassifier.isDigit(cp))
			return numericLiteral();

		if(CharClassifier.isIdentifierStart(cp))
			return identifierOrKeyword();

		error(String.format("invalid character encountered: %s", formatCodepoint(cp)));
		throw new AssertionError("unreachable");
	}

	private Token operatorSeq()
	{
		int before = state.currentOffset == 0 ? ' ' : source[state.currentOffset - 1];

		StringBuilder builder = new StringBuilder();

		do
		{
			builder.appendCodePoint(here());
			advanceColumn();
		}

		while(CharClassifier.isOpChar(here()));

		int after = atEnd() ? ' ' : here();

		boolean leftws = " \t\r\n([{;,".indexOf(before) != -1;
		boolean rightws = " \t\r\n)]};,#".indexOf(after) != -1;

		Token.Type type;

		if(leftws && !rightws)
			type = Token.Type.OPSEQ_PREFIX;
		else if(!leftws && rightws)
			type = Token.Type.OPSEQ_POSTFIX;
		else
			type = Token.Type.OP_INFIX;

		return Token.op(builder.toString(), type);
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
		StringBuilder builder = new StringBuilder();

		while(!atEnd() && here() != '"')
			builder.appendCodePoint(stringCodepoint());

		if(atEnd())
			error("unterminated string literal");

		advanceColumn();

		return Token.stringLiteral(builder.toString());
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

			default: error(String.format("invalid escape sequence \\%s", formatCodepoint(here())));
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

		throw new AssertionError("invalid argument");
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

		case "export":   return Token.DECL_EXPORT;
		case "import":   return Token.DECL_IMPORT;
		case "module":   return Token.DECL_MODULE;
		case "global":   return Token.DECL_GLOBAL;
		case "extern":   return Token.DECL_EXTERN;
		case "fn":       return Token.DECL_FN;
		case "data":     return Token.DECL_DATA;
		case "type":     return Token.DECL_TYPE;
		case "operator": return Token.DECL_OP;
		case "prefix":   return Token.DECL_PREFIX;
		case "infix":    return Token.DECL_INFIX;
		case "postfix":  return Token.DECL_POSTFIX;

		case "local":  return Token.STMT_LOCAL;
		case "if":     return Token.STMT_IF;
		case "unless": return Token.STMT_UNLESS;
		case "else":   return Token.STMT_ELSE;
		case "goto":   return Token.STMT_GOTO;
		case "return": return Token.STMT_RETURN;
		case "loop":   return Token.STMT_LOOP;
		case "while":  return Token.STMT_WHILE;
		case "until":  return Token.STMT_UNTIL;

		case "void":    return Token.TYPE_VOID;
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
		case "opaque":  return Token.TYPE_OPAQUE;
		case "const":   return Token.TYPE_CONST;
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

	private boolean isDigit(int cp, int base)
	{
		if(base < 2 || base > 16)
			throw new AssertionError("invalid argument");

		int index = "0123456789abcdef".indexOf(Character.toLowerCase(cp));
		return index != -1 && index < base;
	}

	private Token numericLiteral()
	{
		StringBuilder literal = new StringBuilder();

		int base = 10;

		if(!atEnd() && here() == '0')
		{
			advanceColumn();

			if(atEnd())
				literal.append('0');

			else if(here() == 'b')
			{
				base = 2;
				advanceColumn();

				if(!isDigit(here(), base))
					error("numeric literal with base prefix requires at least one digit");
			}

			else if(here() == 'o')
			{
				base = 8;
				advanceColumn();

				if(!isDigit(here(), base))
					error("numeric literal with base prefix requires at least one digit");
			}

			else if(here() == 'x')
			{
				base = 16;
				advanceColumn();

				if(!isDigit(here(), base))
					error("numeric literal with base prefix requires at least one digit");
			}

			else if(CharClassifier.isDigit(here()))
				error("numeric literals must start with digit 1-9 or base prefix");

			else
				literal.append('0');
		}


		while(!atEnd() && (isDigit(here(), base) || here() == '\''))
		{
			if(here() != '\'')
				literal.appendCodePoint(here());

			advanceColumn();
		}

		boolean isFloatingPointLiteral = !atEnd() && here() == '.';

		if(isFloatingPointLiteral && base != 10)
			error("base prefixes are not supported with floating point literals");

		if(isFloatingPointLiteral)
		{
			literal.append('.');
			advanceColumn();

			while(!atEnd() && (isDigit(here(), base) || here() == '\''))
			{
				if(here() != '\'')
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

		switch(suffix.toString())
		{
		case "i":   type = Token.Type.LIT_INT;   break;
		case "pi":  type = Token.Type.LIT_PINT;  break;
		case "i8":  type = Token.Type.LIT_INT8;  break;
		case "i16": type = Token.Type.LIT_INT16; break;
		case "i32": type = Token.Type.LIT_INT32; break;
		case "i64": type = Token.Type.LIT_INT64; break;

		case "u":   type = Token.Type.LIT_UINT;   break;
		case "pu":  type = Token.Type.LIT_UPINT;  break;
		case "u8":  type = Token.Type.LIT_UINT8;  break;
		case "u16": type = Token.Type.LIT_UINT16; break;
		case "u32": type = Token.Type.LIT_UINT32; break;
		case "u64": type = Token.Type.LIT_UINT64; break;

		case "f32": type = Token.Type.LIT_FLOAT32; isFloatingPointSuffix = true; break;
		case "f64": type = Token.Type.LIT_FLOAT64; isFloatingPointSuffix = true; break;

		case "":
			type = isFloatingPointLiteral
				? Token.Type.LIT_FLOAT_DEDUCE
				: Token.Type.LIT_INT_DEDUCE;

			isFloatingPointSuffix = isFloatingPointLiteral;
			break;

		default: error(String.format("unknown literal suffix '%s'", suffix));
		}

		if(isFloatingPointLiteral && !isFloatingPointSuffix)
			error("integer suffix used on floating point literal");

		return Token.numericLiteral(type, literal.toString(), base);
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
		return state.currentOffset == source.length;
	}

	private int here()
	{
		return source[state.currentOffset];
	}

	private void skipLineBreak()
	{
		++state.currentOffset;
		++state.line;
		state.currentColumn = 1;
	}

	private void advanceColumn()
	{
		++state.currentOffset;
		++state.currentColumn;
	}

	private void error(String message)
	{
		throw new RuntimeException(String.format("%s on line %s", message, state.line));
	}

	private int[] source;
	private Path path;
	private ScannerState state = new ScannerState();
}
