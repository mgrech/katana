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

package katana.parser;

import katana.ast.DelayedExprParseList;
import katana.ast.type.*;
import katana.diag.CompileException;
import katana.scanner.Scanner;
import katana.scanner.Token;
import katana.utils.Maybe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class TypeParser
{
	public static AstType parse(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		return doParse(scanner, false, delayedExprs);
	}

	private static AstType doParse(Scanner scanner, boolean const_, DelayedExprParseList delayedExprs)
	{
		if(ParseTools.option(scanner, Token.Type.DECL_FN, true))
		{
			if(const_)
				throw new CompileException("forming type 'const function'");

			return parseFunction(scanner, delayedExprs);
		}

		if(ParseTools.option(scanner, Token.Type.PUNCT_LBRACKET, true))
		{
			if(const_)
				throw new CompileException("forming type 'const array'");

			return parseArray(scanner, delayedExprs);
		}

		if(ParseTools.option(scanner, Token.Type.TYPE_OPAQUE, true))
			return parseOpaque(scanner);

		if(ParseTools.option(scanner, Token.Type.TYPE_CONST, true))
		{
			if(const_)
				throw new CompileException("duplicate const");

			return new AstTypeConst(doParse(scanner, true, delayedExprs));
		}

		if(ParseTools.option(scanner, Token.Type.TYPE_TYPEOF, true))
			return parseTypeof(scanner, delayedExprs);

		if(ParseTools.option(scanner, Token.Category.OP, false))
		{
			String ptrs = ParseTools.consume(scanner).value;
			AstType type = parse(scanner, delayedExprs);

			for(char c : new StringBuilder(ptrs).reverse().toString().toCharArray())
			{
				switch(c)
				{
				case '!': type = new AstTypePointer(type); break;
				case '?': type = new AstTypeNullablePointer(type); break;
				default:
					throw new CompileException(String.format("unexpected character '%s' while parsing type", c));
				}
			}

			return type;
		}

		if(ParseTools.option(scanner, Token.Category.TYPE, false))
			return parseBuiltin(scanner);

		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParseTools.consume(scanner).value;
			return new AstTypeUserDefined(name);
		}

		ParseTools.unexpectedToken(scanner, Token.Category.TYPE);
		throw new AssertionError("unreachable");
	}

	private static AstTypeFunction parseFunction(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		List<AstType> params = parseParameters(scanner, delayedExprs);
		Maybe<AstType> ret = Maybe.none();

		if(ParseTools.option(scanner, "=>", true))
			ret = Maybe.some(parse(scanner, delayedExprs));

		return new AstTypeFunction(ret, params);
	}

	private static List<AstType> parseParameters(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		return ParseTools.parenthesized(scanner, () ->
		{
			List<AstType> params = new ArrayList<>();

			if(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
			{
				params.add(parse(scanner, delayedExprs));

				while(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
					params.add(parse(scanner, delayedExprs));
			}

			return params;
		});
	}

	private static AstTypeArray parseArray(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		String size = ParseTools.consumeExpected(scanner, Token.Type.LIT_INT_DEDUCE).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
		return new AstTypeArray(BigInteger.valueOf(Integer.parseInt(size)), TypeParser.parse(scanner, delayedExprs));
	}

	private static AstTypeOpaque parseOpaque(Scanner scanner)
	{
		return ParseTools.parenthesized(scanner, () ->
		{
			String sizeString = ParseTools.consumeExpected(scanner, Token.Type.LIT_INT_DEDUCE).value;
			ParseTools.expect(scanner, Token.Type.PUNCT_COMMA, true);
			String alignmentString = ParseTools.consumeExpected(scanner, Token.Type.LIT_INT_DEDUCE).value;
			BigInteger size = BigInteger.valueOf(Integer.parseInt(sizeString));
			BigInteger alignment = BigInteger.valueOf(Integer.parseInt(alignmentString));
			return new AstTypeOpaque(size, alignment);
		});
	}

	private static AstTypeTypeof parseTypeof(Scanner scanner, DelayedExprParseList delayedExprs)
	{
		return ParseTools.parenthesized(scanner, () -> new AstTypeTypeof(ExprParser.parse(scanner, delayedExprs)));
	}

	private static AstTypeBuiltin parseBuiltin(Scanner scanner)
	{
		Token.Type type = ParseTools.consumeExpected(scanner, Token.Category.TYPE).type;

		switch(type)
		{
		case TYPE_VOID:    return AstTypeBuiltin.VOID;
		case TYPE_BOOL:    return AstTypeBuiltin.BOOL;
		case TYPE_INT8:    return AstTypeBuiltin.INT8;
		case TYPE_INT16:   return AstTypeBuiltin.INT16;
		case TYPE_INT32:   return AstTypeBuiltin.INT32;
		case TYPE_INT64:   return AstTypeBuiltin.INT64;
		case TYPE_INT:     return AstTypeBuiltin.INT;
		case TYPE_PINT:    return AstTypeBuiltin.PINT;
		case TYPE_UINT8:   return AstTypeBuiltin.UINT8;
		case TYPE_UINT16:  return AstTypeBuiltin.UINT16;
		case TYPE_UINT32:  return AstTypeBuiltin.UINT32;
		case TYPE_UINT64:  return AstTypeBuiltin.UINT64;
		case TYPE_UINT:    return AstTypeBuiltin.UINT;
		case TYPE_UPINT:   return AstTypeBuiltin.UPINT;
		case TYPE_FLOAT32: return AstTypeBuiltin.FLOAT32;
		case TYPE_FLOAT64: return AstTypeBuiltin.FLOAT64;

		default: throw new AssertionError("unreachable");
		}
	}
}
