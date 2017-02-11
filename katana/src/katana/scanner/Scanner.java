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

import katana.diag.CompileException;

import java.nio.file.Path;

public class Scanner
{
	private int[] source;
	private Path path;
	private ScannerState state = new ScannerState();

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
			return Tokens.END;

		state.tokenOffset = state.currentOffset;
		state.tokenColumn = state.currentColumn;

		int cp = here();

		switch(cp)
		{
		case ',': advanceColumn(); return Tokens.PUNCT_COMMA;
		case '$': advanceColumn(); return Tokens.PUNCT_DOLLAR;
		case '{': advanceColumn(); return Tokens.PUNCT_LBRACE;
		case '[': advanceColumn(); return Tokens.PUNCT_LBRACKET;
		case '(': advanceColumn(); return Tokens.PUNCT_LPAREN;
		case '}': advanceColumn(); return Tokens.PUNCT_RBRACE;
		case ']': advanceColumn(); return Tokens.PUNCT_RBRACKET;
		case ')': advanceColumn(); return Tokens.PUNCT_RPAREN;
		case ';': advanceColumn(); return Tokens.PUNCT_SCOLON;
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

		TokenType type;

		if(leftws && !rightws)
			type = TokenType.OP_PREFIX_SEQ;
		else if(!leftws && rightws)
			type = TokenType.OP_POSTFIX_SEQ;
		else
			type = TokenType.OP_INFIX;

		return Tokens.op(builder.toString(), type);
	}

	private Token label()
	{
		StringBuilder builder = new StringBuilder();

		while(!atEnd() && CharClassifier.isIdentifierChar(here()))
		{
			builder.appendCodePoint(here());
			advanceColumn();
		}

		return Tokens.label(builder.toString());
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

		return Tokens.stringLiteral(builder.toString());
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
		case "abi":      return Tokens.DECL_ABI;
		case "data":     return Tokens.DECL_DATA;
		case "extern":   return Tokens.DECL_EXTERN;
		case "export":   return Tokens.DECL_EXPORT;
		case "fn":       return Tokens.DECL_FN;
		case "global":   return Tokens.DECL_GLOBAL;
		case "import":   return Tokens.DECL_IMPORT;
		case "infix":    return Tokens.DECL_INFIX;
		case "module":   return Tokens.DECL_MODULE;
		case "operator": return Tokens.DECL_OP;
		case "postfix":  return Tokens.DECL_POSTFIX;
		case "prefix":   return Tokens.DECL_PREFIX;
		case "type":     return Tokens.DECL_TYPE;

		case "false": return Tokens.LIT_BOOL_F;
		case "true":  return Tokens.LIT_BOOL_T;
		case "null":  return Tokens.LIT_NULL;

		case "alignof":      return Tokens.MISC_ALIGNOF;
		case "builtin":      return Tokens.MISC_BUILTIN;
		case "inline":       return Tokens.MISC_INLINE;
		case "narrow_cast":  return Tokens.MISC_NARROW_CAST;
		case "offsetof":     return Tokens.MISC_OFFSETOF;
		case "pointer_cast": return Tokens.MISC_POINTER_CAST;
		case "sign_cast":    return Tokens.MISC_SIGN_CAST;
		case "sizeof":       return Tokens.MISC_SIZEOF;
		case "undef":        return Tokens.MISC_UNDEF;
		case "widen_cast":   return Tokens.MISC_WIDEN_CAST;

		case "else":   return Tokens.STMT_ELSE;
		case "goto":   return Tokens.STMT_GOTO;
		case "if":     return Tokens.STMT_IF;
		case "local":  return Tokens.STMT_LOCAL;
		case "loop":   return Tokens.STMT_LOOP;
		case "return": return Tokens.STMT_RETURN;
		case "unless": return Tokens.STMT_UNLESS;
		case "until":  return Tokens.STMT_UNTIL;
		case "while":  return Tokens.STMT_WHILE;

		case "bool":    return Tokens.TYPE_BOOL;
		case "byte":    return Tokens.TYPE_BYTE;
		case "const":   return Tokens.TYPE_CONST;
		case "float32": return Tokens.TYPE_FLOAT32;
		case "float64": return Tokens.TYPE_FLOAT64;
		case "int":     return Tokens.TYPE_INT;
		case "int8":    return Tokens.TYPE_INT8;
		case "int16":   return Tokens.TYPE_INT16;
		case "int32":   return Tokens.TYPE_INT32;
		case "int64":   return Tokens.TYPE_INT64;
		case "uint":    return Tokens.TYPE_UINT;
		case "uint8":   return Tokens.TYPE_UINT8;
		case "uint16":  return Tokens.TYPE_UINT16;
		case "uint32":  return Tokens.TYPE_UINT32;
		case "uint64":  return Tokens.TYPE_UINT64;
		case "opaque":  return Tokens.TYPE_OPAQUE;
		case "typeof":  return Tokens.TYPE_TYPEOF;
		case "void":    return Tokens.TYPE_VOID;

		default: return Tokens.identifier(value);
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

		TokenType type = null;

		boolean isFloatingPointSuffix = false;

		switch(suffix.toString())
		{
		case "f32": type = TokenType.LIT_FLOAT32; isFloatingPointSuffix = true; break;
		case "f64": type = TokenType.LIT_FLOAT64; isFloatingPointSuffix = true; break;

		case "i":   type = TokenType.LIT_INT;   break;
		case "i8":  type = TokenType.LIT_INT8;  break;
		case "i16": type = TokenType.LIT_INT16; break;
		case "i32": type = TokenType.LIT_INT32; break;
		case "i64": type = TokenType.LIT_INT64; break;

		case "u":   type = TokenType.LIT_UINT;   break;
		case "u8":  type = TokenType.LIT_UINT8;  break;
		case "u16": type = TokenType.LIT_UINT16; break;
		case "u32": type = TokenType.LIT_UINT32; break;
		case "u64": type = TokenType.LIT_UINT64; break;

		case "":
			type = isFloatingPointLiteral
				? TokenType.LIT_FLOAT_DEDUCE
				: TokenType.LIT_INT_DEDUCE;

			isFloatingPointSuffix = isFloatingPointLiteral;
			break;

		default: error(String.format("unknown literal suffix '%s'", suffix));
		}

		if(isFloatingPointLiteral && !isFloatingPointSuffix)
			error("integer suffix used on floating point literal");

		return Tokens.numericLiteral(type, literal.toString(), base);
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
		throw new CompileException(String.format("%s on line %s", message, state.line));
	}
}
