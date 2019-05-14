// Copyright 2016-2019 Markus Grech
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

package io.katana.compiler.parser;

import io.katana.compiler.ast.type.*;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.scanner.TokenCategory;
import io.katana.compiler.scanner.TokenType;
import io.katana.compiler.utils.Maybe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class TypeParser
{
	public static AstType parse(ParseContext ctx)
	{
		return doParse(ctx, false);
	}

	private static AstType doParse(ParseContext ctx, boolean const_)
	{
		if(ParseTools.option(ctx, TokenType.KW_FN, true))
		{
			if(const_)
				ctx.error(-2, ParserDiagnostics.FORMING_CONST_FUNCTION_TYPE);

			return parseFunction(ctx);
		}

		if(ParseTools.option(ctx, TokenType.PUNCT_LBRACE, true))
		{
			if(const_)
				ctx.error(-2, ParserDiagnostics.FORMING_CONST_TUPLE_TYPE);

			return parseTuple(ctx);
		}

		if(ParseTools.option(ctx, TokenType.PUNCT_LBRACKET, true))
		{
			if(const_)
				ctx.error(-2, ParserDiagnostics.FORMING_CONST_ARRAY_TYPE);

			return parseArrayOrSlice(ctx);
		}

		if(ParseTools.option(ctx, TokenType.TYPE_CONST, true))
		{
			if(const_)
			{
				ctx.error(-1, ParserDiagnostics.DUPLICATE_CONST);
				return doParse(ctx, true);
			}

			return new AstTypeConst(doParse(ctx, true));
		}

		if(ParseTools.option(ctx, TokenType.TYPE_TYPEOF, true))
			return parseTypeof(ctx);

		if(ParseTools.option(ctx, TokenCategory.OP, false))
		{
			var token = ParseTools.consume(ctx);
			var type = parse(ctx);

			var count = 0;
			for(var c : ((String)token.value).toCharArray())
			{
				switch(c)
				{
				case '!':
				case '?':
					break;

				default:
					var location = ctx.file().resolve(token.offset + count, 1);
					ctx.error(location, ParserDiagnostics.UNEXPECTED_CHARACTER_IN_TYPE_QUALIFIERS, c);
					break;
				}

				++count;
			}

			for(var c : new StringBuilder((String)token.value).reverse().toString().toCharArray())
			{
				switch(c)
				{
				case '!': type = new AstTypeNonNullablePointer(type); break;
				case '?': type = new AstTypeNullablePointer(type);    break;
				default: break;
				}
			}

			return type;
		}

		if(ParseTools.option(ctx, TokenCategory.TYPE, false))
			return parseBuiltin(ctx);

		if(ParseTools.option(ctx, TokenType.IDENT, false))
		{
			var name = (String)ParseTools.consume(ctx).value;
			return new AstTypeUserDefined(name);
		}

		ParseTools.unexpectedToken(ctx, TokenCategory.TYPE);
		return null;
	}

	private static AstTypeFunction parseFunction(ParseContext ctx)
	{
		var params = parseParameters(ctx);
		Maybe<AstType> ret = Maybe.none();

		if(ParseTools.option(ctx, "=>", true))
			ret = Maybe.some(parse(ctx));

		return new AstTypeFunction(ret, params);
	}

	private static List<AstType> parseParameters(ParseContext ctx)
	{
		return ParseTools.parenthesized(ctx, () ->
		{
			var params = new ArrayList<AstType>();

			if(!ParseTools.option(ctx, TokenType.PUNCT_RPAREN, false))
			{
				params.add(parse(ctx));

				while(!ParseTools.option(ctx, TokenType.PUNCT_RPAREN, false))
					params.add(parse(ctx));
			}

			return params;
		});
	}

	private static AstTypeTuple parseTuple(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.PUNCT_RBRACE, true))
			return new AstTypeTuple(new ArrayList<>());

		var types = new ArrayList<AstType>();
		types.add(parse(ctx));

		while(ParseTools.option(ctx, TokenType.PUNCT_COMMA, true))
			types.add(parse(ctx));

		ParseTools.expect(ctx, TokenType.PUNCT_RBRACE, true);
		return new AstTypeTuple(types);
	}

	private static AstType parseArrayOrSlice(ParseContext ctx)
	{
		if(ParseTools.option(ctx, TokenType.PUNCT_RBRACKET, true))
			return new AstTypeSlice(parse(ctx));

		try
		{
			var size = ((BigInteger)ParseTools.consumeExpected(ctx, TokenType.LIT_INT_DEDUCE).value).longValueExact();
			ParseTools.expect(ctx, TokenType.PUNCT_RBRACKET, true);
			return new AstTypeArray(size, parse(ctx));
		}
		catch(ArithmeticException ex)
		{
			throw new CompileException("array size out of range");
		}
	}

	private static AstTypeTypeof parseTypeof(ParseContext ctx)
	{
		return ParseTools.parenthesized(ctx, () -> new AstTypeTypeof(ExprParser.parse(ctx)));
	}

	private static AstTypeBuiltin parseBuiltin(ParseContext ctx)
	{
		var type = ParseTools.consumeExpected(ctx, TokenCategory.TYPE).type;

		return switch(type)
		{
		case TYPE_VOID    -> AstTypeBuiltin.VOID;
		case TYPE_BYTE    -> AstTypeBuiltin.BYTE;
		case TYPE_BOOL    -> AstTypeBuiltin.BOOL;
		case TYPE_INT8    -> AstTypeBuiltin.INT8;
		case TYPE_INT16   -> AstTypeBuiltin.INT16;
		case TYPE_INT32   -> AstTypeBuiltin.INT32;
		case TYPE_INT64   -> AstTypeBuiltin.INT64;
		case TYPE_INT     -> AstTypeBuiltin.INT;
		case TYPE_UINT8   -> AstTypeBuiltin.UINT8;
		case TYPE_UINT16  -> AstTypeBuiltin.UINT16;
		case TYPE_UINT32  -> AstTypeBuiltin.UINT32;
		case TYPE_UINT64  -> AstTypeBuiltin.UINT64;
		case TYPE_UINT    -> AstTypeBuiltin.UINT;
		case TYPE_FLOAT32 -> AstTypeBuiltin.FLOAT32;
		case TYPE_FLOAT64 -> AstTypeBuiltin.FLOAT64;
		default -> throw new AssertionError("unreachable");
		};
	}
}
