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
import io.katana.compiler.utils.Fraction;
import io.katana.compiler.utils.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Scanner
{
	private final SourceFile file;
	private final DiagnosticsManager diag;
	private int tokenOffset = 0;
	private int groupOffset = 0;
	private int charOffset = 0;

	public static List<Token> tokenize(SourceFile file, DiagnosticsManager diag)
	{
		var result = new ArrayList<Token>();
		var scanner = new Scanner(file, diag);

		for(Token token; (token = scanner.next()) != null;)
			result.add(token.withSourceRange(scanner.tokenOffset, scanner.charOffset - scanner.tokenOffset));

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

			tokenOffset = charOffset;
			var cp = peek();

			switch(cp)
			{
			case ',': skip(); return Tokens.PUNCT_COMMA;
			case '$': skip(); return Tokens.PUNCT_DOLLAR;
			case '{': skip(); return Tokens.PUNCT_LBRACE;
			case '[': skip(); return Tokens.PUNCT_LBRACKET;
			case '(': skip(); return Tokens.PUNCT_LPAREN;
			case '}': skip(); return Tokens.PUNCT_RBRACE;
			case ']': skip(); return Tokens.PUNCT_RBRACKET;
			case ')': skip(); return Tokens.PUNCT_RPAREN;
			case ';': skip(); return Tokens.PUNCT_SCOLON;
			case '@': skip(); return label();
			default: break;
			}

			if(cp == '"')
			{
				skip();
				return stringLiteral();
			}

			var cps = file.codepoints();

			if(CharClassifier.isDecDigit(cp) || cp == '.' && charOffset + 1 < cps.length && CharClassifier.isDecDigit(cps[charOffset + 1]))
				return numericLiteral();

			if(CharClassifier.isOpChar(cp))
				return operatorSeq();

			if(CharClassifier.isIdentifierHead(cp))
				return identifierOrKeyword();

			skip();
			raiseCharError(ScannerDiagnostics.INVALID_CODEPOINT, StringUtils.formatCodepoint(cp));
		}
	}

	private Token operatorSeq()
	{
		var before = charOffset == 0 ? ' ' : file.codepoints()[charOffset - 1];

		var builder = new StringBuilder();

		do builder.appendCodePoint(consume());
		while(!eof() && CharClassifier.isOpChar(peek()));

		var after = eof() ? ' ' : peek();

		var leftws  = " \t\r\n([{;,".indexOf(before) != -1;
		var rightws = " \t\r\n)]};,#".indexOf(after) != -1;

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
		var builder = new StringBuilder();

		while(!eof() && CharClassifier.isIdentifierTail(peek()))
			builder.appendCodePoint(consume());

		if(builder.length() == 0)
		{
			raiseTokenError(ScannerDiagnostics.EMPTY_LABEL);
			return Tokens.label(null);
		}

		return Tokens.label(builder.toString());
	}

	private Token stringLiteral()
	{
		var invalid = false;
		var valueBuilder = new StringBuilder();

		while(!eof() && peek() != '"' && peek() != '\r' && peek() != '\n')
		{
			var cp = stringCodepoint();

			if(cp != -1)
				valueBuilder.appendCodePoint(cp);
			else
				invalid = true;
		}

		if(eof() || peek() == '\r' || peek() == '\n')
		{
			raiseTokenError(ScannerDiagnostics.UNTERMINATED_STRING);
			invalid = true;
		}
		else
			skip();

		return Tokens.stringLiteral(invalid ? null : valueBuilder.toString());
	}

	private int stringCodepoint()
	{
		if(peek() != '\\')
			return consume();

		markGroup();
		skip();

		if(eof())
			return -1;

		var escape = consume();

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

		case 'U': return unicodeEscape(true);
		case 'u': return unicodeEscape(false);
		case 'x': return hexEscape();

		default: break;
		}

		raiseGroupError(ScannerDiagnostics.INVALID_ESCAPE, StringUtils.formatCodepoint(escape));
		return -1;
	}

	private int unicodeEscape(boolean big)
	{
		var tooShortError = ScannerDiagnostics.UNICODE_ESCAPE_TOO_SHORT;
		var expectedLength = big ? 6 : 4;

		var d1 = (big ? hexDigit(tooShortError, expectedLength) : 0) << 20;
		var d2 = (big ? hexDigit(tooShortError, expectedLength) : 0) << 16;
		var d3 = hexDigit(tooShortError, expectedLength) << 12;
		var d4 = hexDigit(tooShortError, expectedLength) <<  8;
		var d5 = hexDigit(tooShortError, expectedLength) <<  4;
		var d6 = hexDigit(tooShortError, expectedLength);
		var sum =  d1 | d2 | d3 | d4 | d5 | d6;

		if(sum < 0)
			return -1;

		if(sum > 0x10FFFF)
		{
			raiseGroupError(ScannerDiagnostics.CODEPOINT_OUT_OF_RANGE);
			return -1;
		}

		return sum;
	}

	private int hexEscape()
	{
		var tooShortError = ScannerDiagnostics.HEX_ESCAPE_TOO_SHORT;

		var d1 = hexDigit(tooShortError, 2);
		var d2 = hexDigit(tooShortError, 2);

		if(d1 == -1 || d2 == -1)
			return -1;

		return 16 * d1 + d2;
	}

	private int hexDigit(DiagnosticId tooShortError, Object... errorArgs)
	{
		if(eof() || peek() == '\r' || peek() == '\n')
			return -1;

		if(peek() == '"')
		{
			raiseGroupError(tooShortError, errorArgs);
			return -1;
		}

		if(!CharClassifier.isHexDigit(peek()))
		{
			skip();
			raiseCharError(ScannerDiagnostics.INVALID_CHARACTER_IN_ESCAPE);
			return -1;
		}

		return fromHexDigit(consume());
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
		var builder = new StringBuilder();

		do builder.appendCodePoint(consume());
		while(!eof() && CharClassifier.isIdentifierTail(peek()));

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
		case "opaque":   return Tokens.DECL_OPAQUE;
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
		case "typeof":  return Tokens.TYPE_TYPEOF;
		case "void":    return Tokens.TYPE_VOID;

		default: return Tokens.identifier(value);
		}
	}

	private Token numericLiteral()
	{
		var invalid = false;
		var literal = new StringBuilder();

		var base = 10;

		if(!eof() && peek() == '0')
		{
			skip();

			if(eof())
				literal.append('0');
			else if(peek() == 'b')
			{
				base = 2;
				skip();
			}
			else if(peek() == 'o')
			{
				base = 8;
				skip();
			}
			else if(peek() == 'x')
			{
				base = 16;
				skip();
			}
			else if(CharClassifier.isDecDigit(peek()))
			{
				while(!eof() && peek() == '0')
					skip();

				raiseTokenError(ScannerDiagnostics.INVALID_START_IN_NUMERIC_LITERAL);
				invalid = true;
			}
			else
				literal.append('0');
		}

		while(!eof() && (CharClassifier.isAnyDigit(peek()) || peek() == '\''))
		{
			var cp = consume();

			if(cp != '\'')
			{
				literal.appendCodePoint(cp);

				if(!(CharClassifier.isDigit(cp, base)))
				{
					raiseCharError(ScannerDiagnostics.INVALID_DIGIT_FOR_BASE, base);
					invalid = true;
				}
			}
		}

		while(literal.length() > 1 && literal.charAt(0) == '0')
			literal.deleteCharAt(0);

		var isFloatingPointLiteral = !eof() && peek() == '.';

		if(isFloatingPointLiteral)
		{
			literal.append('.');
			skip();

			while(!eof() && (CharClassifier.isDigit(peek(), base) || peek() == '\''))
			{
				int cp = consume();

				if(cp != '\'')
					literal.appendCodePoint(cp);
			}
		}

		if(literal.length() == 0)
		{
			raiseTokenError(ScannerDiagnostics.EMPTY_NUMERIC_LITERAL);
			invalid = true;
			literal.append('0');
		}

		if(isFloatingPointLiteral && base != 10)
		{
			raiseError(tokenOffset, 2, ScannerDiagnostics.BASE_PREFIX_ON_FLOAT_LITERAL);
			invalid = true;
			literal = new StringBuilder("0");
		}

		var suffix = new StringBuilder();

		if(!eof() && peek() == '$')
		{
			skip();
			markGroup();

			while(!eof() && CharClassifier.isIdentifierTail(peek()))
				suffix.appendCodePoint(consume());

			if(suffix.length() == 0)
			{
				raiseCharError(ScannerDiagnostics.EMPTY_SUFFIX);
				invalid = true;
			}
		}

		TokenType type;
		var isFloatingPointSuffix = false;

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
			type = isFloatingPointLiteral ? TokenType.LIT_FLOAT_DEDUCE : TokenType.LIT_INT_DEDUCE;
			isFloatingPointSuffix = isFloatingPointLiteral;
			break;

		default:
			raiseGroupError(ScannerDiagnostics.INVALID_LITERAL_SUFFIX, suffix);
			invalid = true;
			type = isFloatingPointLiteral ? TokenType.LIT_FLOAT_DEDUCE : TokenType.LIT_INT_DEDUCE;
			break;
		}

		if(isFloatingPointLiteral && !isFloatingPointSuffix)
		{
			raiseGroupError(ScannerDiagnostics.INT_SUFFIX_ON_FLOAT_LITERAL);
			invalid = true;
			type = TokenType.LIT_FLOAT_DEDUCE;
		}

		var value = invalid ? null : isFloatingPointLiteral
		                             ? floatingPointLiteralToFraction(literal.toString(), base)
		                             : new BigInteger(literal.toString(), base);

		return Tokens.numericLiteral(type, value);
	}

	private Fraction floatingPointLiteralToFraction(String literal, int base)
	{
		var parts = literal.split("\\.", 2);
		var bigbase = BigInteger.valueOf(base);

		var numerator = new BigInteger(parts[0].isEmpty() ? "0" : parts[0]);
		var denominator = BigInteger.ONE;

		if(parts.length != 1)
		{
			while(parts[1].endsWith("0"))
				parts[1] = parts[1].substring(0, parts[1].length() - 1);

			for(var digit : parts[1].toCharArray())
			{
				var value = BigInteger.valueOf(digit - '0');
				numerator = numerator.multiply(bigbase).add(value);
				denominator = denominator.multiply(bigbase);
			}
		}

		return Fraction.of(numerator, denominator);
	}

	private void skipWhitespaceAndComments()
	{
		do skipWhitespace();
		while(skipComment());
	}

	private void skipWhitespace()
	{
		while(!eof() && (CharClassifier.isWhitespace(peek()) || CharClassifier.isLineBreak(peek())))
			skip();
	}

	private boolean skipComment()
	{
		if(eof() || peek() != '#')
			return false;

		skip();

		if(eof())
			return true;

		var cp = consume();

		if(cp == '{')
		{
			skipMultiLineComment();
		}
		else if(!CharClassifier.isLineBreak(cp))
		{
			while(!eof() && !CharClassifier.isLineBreak(peek()))
				skip();
		}

		return true;
	}

	private void skipMultiLineComment()
	{
		var level = 1;

		for(;;)
		{
			while(!eof() && peek() != '#')
				skip();

			if(eof())
			{
				raiseTokenError(ScannerDiagnostics.UNTERMINATED_MULTILINE_COMMENT);
				return;
			}

			skip();

			if(eof())
			{
				raiseTokenError(ScannerDiagnostics.UNTERMINATED_MULTILINE_COMMENT);
				return;
			}

			if(peek() == '}')
			{
				skip();
				--level;

				if(level == 0)
					return;
			}
			else if(peek() == '{')
			{
				skip();
				++level;
			}
		}
	}

	private boolean eof()
	{
		return charOffset == file.codepoints().length;
	}

	private int peek()
	{
		return file.codepoints()[charOffset];
	}

	private void skip()
	{
		++charOffset;
	}

	private int consume()
	{
		var cp = peek();
		skip();
		return cp;
	}

	private void markGroup()
	{
		groupOffset = charOffset;
	}

	private void raiseError(int fileOffset, int length, DiagnosticId id, Object... args)
	{
		var location = file.resolve(fileOffset, length);
		diag.error(location, id, args);
	}

	private void raiseCharError(DiagnosticId id, Object... args)
	{
		raiseError(charOffset - 1, 1, id, args);
	}

	private void raiseGroupError(DiagnosticId id, Object... args)
	{
		raiseError(groupOffset, charOffset - groupOffset, id, args);
	}

	private void raiseTokenError(DiagnosticId id, Object... args)
	{
		raiseError(tokenOffset, charOffset - tokenOffset, id, args);
	}
}
