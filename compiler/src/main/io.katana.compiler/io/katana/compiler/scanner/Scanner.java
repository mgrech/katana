// Copyright 2016-2018 Markus Grech
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

import io.katana.compiler.diag.DiagnosticId;
import io.katana.compiler.diag.DiagnosticsManager;
import io.katana.compiler.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Scanner
{
	private final SourceFile file;
	private final DiagnosticsManager diag;
	private int prevOffset = 0;
	private int currOffset = 0;

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

	private Token next()
	{
		for(;;)
		{
			skipWhitespaceAndComments();

			if(eof())
				return null;

			prevOffset = currOffset;

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
				return stringLiteral();
			}

			int[] cps = file.codepoints();

			if(CharClassifier.isDecDigit(cp) || cp == '.' && currOffset + 1 < cps.length && CharClassifier.isDecDigit(cps[currOffset + 1]))
				return numericLiteral();

			if(CharClassifier.isOpChar(cp))
				return operatorSeq();

			if(CharClassifier.isIdentifierHead(cp))
				return identifierOrKeyword();

			advance();
			error(ScannerDiagnostics.INVALID_CODEPOINT, StringUtils.formatCodepoint(cp));
		}
	}

	private Token operatorSeq()
	{
		int before = currOffset == 0 ? ' ' : file.codepoints()[currOffset - 1];

		StringBuilder builder = new StringBuilder();

		do
		{
			builder.appendCodePoint(here());
			advance();
		}
		while(!eof() && CharClassifier.isOpChar(here()));

		int after = eof() ? ' ' : here();

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

		while(!eof() && CharClassifier.isIdentifierTail(here()))
		{
			builder.appendCodePoint(here());
			advance();
		}

		return Tokens.label(builder.toString());
	}

	private Token stringLiteral()
	{
		boolean invalid = false;
		StringBuilder valueBuilder = new StringBuilder();

		while(!eof() && here() != '"' && here() != '\r' && here() != '\n')
		{
			int cp = stringCodepoint();

			if(cp != -1)
				valueBuilder.appendCodePoint(cp);
			else
				invalid = true;
		}

		if(eof() || here() == '\r' || here() == '\n')
		{
			error(ScannerDiagnostics.UNTERMINATED_STRING);
			invalid = true;
		}
		else
			advance();

		StringBuilder tokenBuilder = new StringBuilder();

		for(int i = prevOffset; i != currOffset; ++i)
			tokenBuilder.appendCodePoint(file.codepoints()[i]);

		return Tokens.stringLiteral(tokenBuilder.toString(), invalid ? null : valueBuilder.toString());
	}

	private int stringCodepoint()
	{
		if(here() == '\\')
		{
			advance();

			if(eof())
				return -1;

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

			case '"':  return '"';
			case '\\': return '\\';

			default: break;
			}

			// for error handling, temporarily pretend the token starts at the \ character
			int tmp = prevOffset;
			prevOffset = currOffset - 2;

			int result = -1;

			switch(escape)
			{
			case 'U': result = unicodeEscape(true);  break;
			case 'u': result = unicodeEscape(false); break;
			case 'x': result = hexEscape(ScannerDiagnostics.HEX_ESCAPE_TOO_SHORT, 2); break;

			default:
				int cp = file.codepoints()[currOffset - 1];
				error(ScannerDiagnostics.INVALID_ESCAPE, StringUtils.formatCodepoint(cp));
				break;
			}

			// restore original token offset
			prevOffset = tmp;

			return result;
		}

		int cp = here();
		advance();
		return cp;
	}

	private int unicodeEscape(boolean big)
	{
		var tooShortError = ScannerDiagnostics.UNICODE_ESCAPE_TOO_SHORT;
		var expectedLength = big ? 6 : 4;

		int d1 = (big ? hexDigit(tooShortError, expectedLength) : 0) << 20;
		int d2 = (big ? hexDigit(tooShortError, expectedLength) : 0) << 16;
		int d3 = hexDigit(tooShortError, expectedLength) << 12;
		int d4 = hexDigit(tooShortError, expectedLength) <<  8;
		int d5 = hexDigit(tooShortError, expectedLength) <<  4;
		int d6 = hexDigit(tooShortError, expectedLength);
		int sum =  d1 | d2 | d3 | d4 | d5 | d6;

		if(sum < 0)
			return -1;

		if(sum > 0x10FFFF)
		{
			error(ScannerDiagnostics.CODEPOINT_OUT_OF_RANGE);
			return -1;
		}

		return sum;
	}

	private int hexEscape(DiagnosticId tooShortError, Object... errorArgs)
	{
		int d1 = hexDigit(tooShortError, errorArgs);
		int d2 = hexDigit(tooShortError, errorArgs);

		if(d1 == -1 || d2 == -1)
			return -1;

		return 16 * d1 + d2;
	}

	private int hexDigit(DiagnosticId tooShortError, Object... errorArgs)
	{
		if(eof() || here() == '\r' || here() == '\n')
			return -1;

		if(here() == '"')
		{
			error(tooShortError, errorArgs);
			return -1;
		}

		if(!CharClassifier.isHexDigit(here()))
		{
			int tmp = prevOffset;
			prevOffset = currOffset;
			++currOffset;
			error(ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE);
			--currOffset;
			prevOffset = tmp;

			advance();
			return -1;
		}

		int digit = fromHexDigit(here());
		advance();
		return digit;
	}

	private int fromHexDigit(int cp)
	{
		if(CharClassifier.isDecDigit(cp))
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
		while(!eof() && CharClassifier.isIdentifierTail(here()));

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

	private Token numericLiteral()
	{
		boolean invalid = false;
		StringBuilder literal = new StringBuilder();

		int base = 10;

		if(!eof() && here() == '0')
		{
			advance();

			if(eof())
				literal.append('0');

			else if(here() == 'b')
			{
				base = 2;
				advance();
			}

			else if(here() == 'o')
			{
				base = 8;
				advance();
			}

			else if(here() == 'x')
			{
				base = 16;
				advance();
			}

			else if(CharClassifier.isDecDigit(here()))
			{
				while(!eof() && here() == '0')
					advance();

				error(ScannerDiagnostics.INVALID_START_IN_NUMERIC_LITERAL);
				prevOffset = currOffset;
			}

			else
				literal.append('0');
		}

		while(!eof() && (CharClassifier.isAnyDigit(here()) || here() == '\''))
		{
			if(here() != '\'')
			{
				literal.appendCodePoint(here());

				if(!(CharClassifier.isDigit(here(), base)))
				{
					int tmpPrevOffset = prevOffset;
					int tmpOffset = currOffset;

					prevOffset = currOffset;
					++currOffset;
					error(ScannerDiagnostics.INVALID_DIGIT_FOR_BASE, base);
					invalid = true;

					prevOffset = tmpPrevOffset;
					currOffset = tmpOffset;
				}
			}

			advance();
		}

		while(literal.length() > 1 && literal.charAt(0) == '0')
			literal.deleteCharAt(0);

		boolean isFloatingPointLiteral = !eof() && here() == '.';

		if(isFloatingPointLiteral)
		{
			literal.append('.');
			advance();

			while(!eof() && (CharClassifier.isDigit(here(), base) || here() == '\''))
			{
				if(here() != '\'')
					literal.appendCodePoint(here());

				advance();
			}
		}

		if(literal.length() == 0)
		{
			error(ScannerDiagnostics.EMPTY_NUMERIC_LITERAL);
			invalid = true;
			literal.append('0');
		}

		StringBuilder suffix = new StringBuilder();

		if(!eof() && here() == '$')
		{
			int suffixOffset = currOffset;

			advance();

			while(!eof() && CharClassifier.isIdentifierTail(here()))
			{
				suffix.appendCodePoint(here());
				advance();
			}

			if(suffix.length() == 0)
			{
				int tmp = prevOffset;

				prevOffset = suffixOffset;
				error(ScannerDiagnostics.EMPTY_SUFFIX);
				invalid = true;

				prevOffset = tmp;
			}
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
			int tmp = prevOffset;

			prevOffset = currOffset - suffix.length();
			error(ScannerDiagnostics.INVALID_LITERAL_SUFFIX, suffix);
			invalid = true;

			prevOffset = tmp;

			type = isFloatingPointLiteral ? TokenType.LIT_FLOAT_DEDUCE : TokenType.LIT_INT_DEDUCE;
			break;
		}

		if(isFloatingPointLiteral && base != 10)
		{
			currOffset -= literal.length();
			error(ScannerDiagnostics.BASE_PREFIX_ON_FLOAT_LITERAL);
			invalid = true;
			currOffset += literal.length();
			literal = new StringBuilder("0");
		}

		if(isFloatingPointLiteral && !isFloatingPointSuffix)
		{
			int tmp = prevOffset;
			prevOffset = currOffset - suffix.length();
			error(ScannerDiagnostics.INT_SUFFIX_ON_FLOAT_LITERAL);
			invalid = true;
			prevOffset = tmp;

			type = TokenType.LIT_FLOAT_DEDUCE;
		}

		return Tokens.numericLiteral(type, invalid ? null : literal.toString(), base);
	}

	private void skipWhitespaceAndComments()
	{
		for(;;)
		{
			skipWhitespace();

			if(!eof() && here() == '#')
				skipComment();

			if(eof() || !CharClassifier.isLineBreak(here()))
				break;

			advance();
		}
	}

	private void skipWhitespace()
	{
		while(!eof() && CharClassifier.isWhitespace(here()))
			advance();
	}

	private void skipComment()
	{
		do advance();
		while(!eof() && !CharClassifier.isLineBreak(here()));
	}

	private boolean eof()
	{
		return currOffset == file.codepoints().length;
	}

	private int here()
	{
		return file.codepoints()[currOffset];
	}

	private void advance()
	{
		++currOffset;
	}

	private void error(DiagnosticId id, Object... args)
	{
		error(0, id, args);
	}

	private void error(int offset, DiagnosticId id, Object... args)
	{
		error(offset, currOffset - prevOffset, id, args);
	}

	private void error(int offset, int length, DiagnosticId id, Object... args)
	{
		var location = file.resolve(prevOffset - offset, length);
		diag.error(location, id, args);
	}
}
