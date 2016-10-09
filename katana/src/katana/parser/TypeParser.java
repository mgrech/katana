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

import katana.ast.type.*;
import katana.scanner.Scanner;
import katana.scanner.Token;
import katana.utils.Maybe;

import java.math.BigInteger;
import java.util.ArrayList;

public class TypeParser
{
	public static AstType parse(Scanner scanner)
	{
		return doParse(scanner, false);
	}

	private static AstType doParse(Scanner scanner, boolean const_)
	{
		if(ParseTools.option(scanner, Token.Type.DECL_FN, true))
		{
			if(const_)
				throw new RuntimeException("forming type 'const function'");

			return parseFunction(scanner);
		}

		if(ParseTools.option(scanner, Token.Type.PUNCT_LBRACKET, true))
		{
			if(const_)
				throw new RuntimeException("forming type 'const array'");

			return parseArray(scanner);
		}

		if(ParseTools.option(scanner, Token.Type.TYPE_OPAQUE, true))
			return parseOpaque(scanner);

		if(ParseTools.option(scanner, Token.Type.TYPE_CONST, true))
		{
			if(const_)
				throw new RuntimeException("duplicate const");

			return new AstTypeConst(doParse(scanner, true));
		}

		if(ParseTools.option(scanner, Token.Type.TYPE_TYPEOF, true))
			return parseTypeof(scanner);

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

	private static AstTypeFunction parseFunction(Scanner scanner)
	{
		ArrayList<AstType> params = parseParameters(scanner);
		Maybe<AstType> ret = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.PUNCT_RET, true))
			ret = Maybe.some(parse(scanner));

		return new AstTypeFunction(ret, params);
	}

	private static ArrayList<AstType> parseParameters(Scanner scanner)
	{
		return ParseTools.parenthesized(scanner, () ->
		{
			ArrayList<AstType> params = new ArrayList<>();

			if(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
			{
				params.add(parse(scanner));

				while(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
					params.add(parse(scanner));
			}

			return params;
		});
	}

	private static AstTypeArray parseArray(Scanner scanner)
	{
		String size = ParseTools.consumeExpected(scanner, Token.Type.LIT_INT_DEDUCE).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
		return new AstTypeArray(BigInteger.valueOf(Integer.parseInt(size)), TypeParser.parse(scanner));
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

	private static AstTypeTypeof parseTypeof(Scanner scanner)
	{
		return ParseTools.parenthesized(scanner, () -> new AstTypeTypeof(ExprParser.parse(scanner)));
	}

	private static AstTypeBuiltin parseBuiltin(Scanner scanner)
	{
		Token.Type type = ParseTools.consumeExpected(scanner, Token.Category.TYPE).type;

		switch(type)
		{
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
		case TYPE_PTR:     return AstTypeBuiltin.PTR;

		default: throw new AssertionError("unreachable");
		}
	}
}
