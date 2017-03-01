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

import katana.diag.DiagnosticsManager;
import katana.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Scanner
{
	private final SourceFile file;
	private final DiagnosticsManager diag;
	private int prevOffset = 0;
	private int offset = 0;

	public static List<Token> tokenize(SourceFile file, DiagnosticsManager diag)
	{
		List<Token> result = new ArrayList<>();
		Scanner scanner = new Scanner(file, diag);

		for(Token token; (token = scanner.next()) != null;)
			result.add(token.withOffset(scanner.prevOffset));

		return result;
	}

	private Scanner(SourceFile file, DiagnosticsManager diag)
	{
		this.file = file;
		this.diag = diag;
	}

	private SourceLocation location()
	{
		return file.resolve(prevOffset, offset - prevOffset);
	}

	private Token next()
	{
		for(;;)
		{
			skipWhitespaceAndComments();

			if(atEnd())
				return null;

			prevOffset = offset;

			int cp = here();

			switch(cp)
			{
			case ',': advance(); return Tokens.PUNCT_COMMA;
			case '$': advance(); return Tokens.PUNCT_DOLLAR;
			case '{': advance(); return Tokens.PUNCT_LBRACE;
			case '[': advance(); return Tokens.PUNCT_LBRACKET;
			case '(': advance(); return Tokens.PUNCT_LPAREN;
			case '}': advance(); return Tokens.PUNCT_RBRACE;
			case ']': advance(); return Tokens.PUNCT_RBRACKET;
			case ')': advance(); return Tokens.PUNCT_RPAREN;
			case ';': advance(); return Tokens.PUNCT_SCOLON;
			case '@': advance(); return label();
			default: break;
			}

			if(cp == '"')
			{
				advance();
				Token lit = stringLiteral();

				if(lit == null)
					continue;

				return lit;
			}

			if(CharClassifier.isOpChar(cp))
				return operatorSeq();

			if(CharClassifier.isDigit(cp))
			{
				Token lit = numericLiteral();

				if(lit == null)
					continue;

				return lit;
			}

			if(CharClassifier.isIdentifierHead(cp))
				return identifierOrKeyword();

			advance();
			error("invalid codepoint encountered: %s", StringUtils.formatCodepoint(cp));
		}
	}

	private Token operatorSeq()
	{
		int before = offset == 0 ? ' ' : file.codepoints()[offset - 1];

		StringBuilder builder = new StringBuilder();

		do
		{
			builder.appendCodePoint(here());
			advance();
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

		while(!atEnd() && CharClassifier.isIdentifierTail(here()))
		{
			builder.appendCodePoint(here());
			advance();
		}

		return Tokens.label(builder.toString());
	}

	private Token stringLiteral()
	{
		StringBuilder builder = new StringBuilder();

		while(!atEnd() && here() != '"' && here() != '\r' && here() != '\n')
		{
			int cp = stringCodepoint();

			if(cp != -1)
				builder.appendCodePoint(cp);
		}

		if(atEnd() || here() == '\r' || here() == '\n')
		{
			error("unterminated string literal");
			return null;
		}

		advance();

		StringBuilder tokenBuilder = new StringBuilder();

		for(int i = prevOffset; i != offset; ++i)
			tokenBuilder.appendCodePoint(file.codepoints()[i]);

		return Tokens.stringLiteral(tokenBuilder.toString(), builder.toString());
	}

	private int stringCodepoint()
	{
		if(here() == '\\')
		{
			advance();

			int escape = here();
			advance();

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

			default: break;
			}

			--offset;
			error("invalid escape sequence \\%s", StringUtils.formatCodepoint(here()));
			++offset;
			return -1;
		}

		int cp = here();
		advance();
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
		{
			error("invalid codepoint in unicode escape sequence");
			return -1;
		}

		return sum;
	}

	private int hexEscape()
	{
		int d1 = hexDigit();
		int d2 = hexDigit();

		if(d1 == -1 || d2 == -1)
			return -1;

		return 16 * d1 + d2;
	}

	private int hexDigit()
	{
		if(atEnd() || !CharClassifier.isHexDigit(here()))
		{
			error("expected hex digit in escape sequence");
			return -1;
		}

		int digit = fromHexDigit(here());
		advance();
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
			advance();
		}
		while(!atEnd() && CharClassifier.isIdentifierTail(here()));

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
			advance();

			if(atEnd())
				literal.append('0');

			else if(here() == 'b')
			{
				base = 2;
				advance();

				if(!isDigit(here(), base))
				{
					error("numeric literal with base prefix requires at least one digit");
					return null;
				}
			}

			else if(here() == 'o')
			{
				base = 8;
				advance();

				if(!isDigit(here(), base))
				{
					error("numeric literal with base prefix requires at least one digit");
					return null;
				}
			}

			else if(here() == 'x')
			{
				base = 16;
				advance();

				if(!isDigit(here(), base))
				{
					error("numeric literal with base prefix requires at least one digit");
					return null;
				}
			}

			else if(CharClassifier.isDigit(here()))
			{
				error("numeric literals must start with digit 1-9 or base prefix");
				return null;
			}

			else
				literal.append('0');
		}

		while(!atEnd() && (isDigit(here(), base) || here() == '\''))
		{
			if(here() != '\'')
				literal.appendCodePoint(here());

			advance();
		}

		boolean isFloatingPointLiteral = !atEnd() && here() == '.';

		if(isFloatingPointLiteral && base != 10)
		{
			error("base prefixes are not supported with floating point literals");
			base = 10;
		}

		if(isFloatingPointLiteral)
		{
			literal.append('.');
			advance();

			while(!atEnd() && (isDigit(here(), base) || here() == '\''))
			{
				if(here() != '\'')
					literal.appendCodePoint(here());

				advance();
			}
		}

		StringBuilder suffix = new StringBuilder();

		while(!atEnd() && (CharClassifier.isIdentifierTail(here()) || CharClassifier.isDigit(here())))
		{
			suffix.appendCodePoint(here());
			advance();
		}

		TokenType type;

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
			type = isFloatingPointLiteral ? TokenType.LIT_FLOAT_DEDUCE  : TokenType.LIT_INT_DEDUCE;
			isFloatingPointSuffix = isFloatingPointLiteral;
			break;

		default:
			error("unknown literal suffix '%s'", suffix);
			type = isFloatingPointLiteral ? TokenType.LIT_FLOAT_DEDUCE : TokenType.LIT_INT_DEDUCE;
			break;
		}

		if(isFloatingPointLiteral && !isFloatingPointSuffix)
		{
			error("integer suffix used on floating point literal");
			type = TokenType.LIT_FLOAT_DEDUCE;
		}

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

			advance();
		}
	}

	private void skipWhitespace()
	{
		while(!atEnd() && CharClassifier.isWhitespace(here()))
			advance();
	}

	private void skipComment()
	{
		do advance();
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
		return offset == file.codepoints().length;
	}

	private int here()
	{
		return file.codepoints()[offset];
	}

	private void advance()
	{
		++offset;
	}

	private void error(String fmt, Object... args)
	{
		diag.error(location(), fmt, args);
	}
}
