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

import katana.utils.Maybe;
import katana.ast.Type;
import katana.ast.type.*;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.util.ArrayList;

public class TypeParser
{
	public static Type parse(Scanner scanner)
	{
		if(ParseTools.option(scanner, Token.Type.DECL_FN, true))
			return parseFunction(scanner);

		if(ParseTools.option(scanner, Token.Type.PUNCT_LBRACKET, true))
			return parseArray(scanner);

		if(ParseTools.option(scanner, Token.Type.TYPE_OPAQUE, true))
			return parseOpaque(scanner);

		if(ParseTools.option(scanner, Token.Type.TYPE_TYPEOF, true))
			return parseTypeof(scanner);

		if(ParseTools.option(scanner, Token.Category.TYPE, false))
			return parseBuiltin(scanner);

		if(ParseTools.option(scanner, Token.Type.IDENT, false))
		{
			String name = ParseTools.consume(scanner).value;
			return new UserDefined(name);
		}

		ParseTools.unexpectedToken(scanner, Token.Category.TYPE);
		throw new AssertionError("unreachable");
	}

	private static Function parseFunction(Scanner scanner)
	{
		ArrayList<Type> params = parseParameters(scanner);
		Maybe<Type> ret = Maybe.none();

		if(ParseTools.option(scanner, Token.Type.PUNCT_RET, true))
			ret = Maybe.some(parse(scanner));

		return new Function(ret, params);
	}

	private static ArrayList<Type> parseParameters(Scanner scanner)
	{
		return ParseTools.parenthesized(scanner, () ->
		{
			ArrayList<Type> params = new ArrayList<>();

			if(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
			{
				params.add(parse(scanner));

				while(!ParseTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
					params.add(parse(scanner));
			}

			return params;
		});
	}

	private static Array parseArray(Scanner scanner)
	{
		String size = ParseTools.consumeExpected(scanner, Token.Type.LIT_INT).value;
		ParseTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
		return new Array(Integer.parseInt(size), TypeParser.parse(scanner));
	}

	private static Opaque parseOpaque(Scanner scanner)
	{
		return ParseTools.parenthesized(scanner, () ->
		{
			String size = ParseTools.consumeExpected(scanner, Token.Type.LIT_INT).value;
			ParseTools.expect(scanner, Token.Type.PUNCT_COMMA, true);
			String alignment = ParseTools.consumeExpected(scanner, Token.Type.LIT_INT).value;
			return new Opaque(Integer.parseInt(size), Integer.parseInt(alignment));
		});
	}

	private static Typeof parseTypeof(Scanner scanner)
	{
		return ParseTools.parenthesized(scanner, () -> new Typeof(ExprParser.parse(scanner)));
	}

	private static Builtin parseBuiltin(Scanner scanner)
	{
		Token.Type type = ParseTools.consumeExpected(scanner, Token.Category.TYPE).type;

		switch(type)
		{
		case TYPE_BOOL:    return Builtin.BOOL;
		case TYPE_INT8:    return Builtin.INT8;
		case TYPE_INT16:   return Builtin.INT16;
		case TYPE_INT32:   return Builtin.INT32;
		case TYPE_INT64:   return Builtin.INT64;
		case TYPE_INT:     return Builtin.INT;
		case TYPE_PINT:    return Builtin.PINT;
		case TYPE_UINT8:   return Builtin.UINT8;
		case TYPE_UINT16:  return Builtin.UINT16;
		case TYPE_UINT32:  return Builtin.UINT32;
		case TYPE_UINT64:  return Builtin.UINT64;
		case TYPE_UINT:    return Builtin.UINT;
		case TYPE_UPINT:   return Builtin.UPINT;
		case TYPE_FLOAT32: return Builtin.FLOAT32;
		case TYPE_FLOAT64: return Builtin.FLOAT64;
		case TYPE_PTR:     return Builtin.PTR;

		default: throw new AssertionError("unreachable");
		}
	}
}
