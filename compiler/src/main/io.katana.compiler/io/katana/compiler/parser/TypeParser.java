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

package io.katana.compiler.parser;

import io.katana.compiler.ast.type.*;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.scanner.SourceLocation;
import io.katana.compiler.scanner.Token;
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
		if(ParseTools.option(ctx, TokenType.DECL_FN, true))
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
			Token token = ParseTools.consume(ctx);
			AstType type = parse(ctx);

			int count = 0;
			for(char c : ((String)token.value).toCharArray())
			{
				switch(c)
				{
				case '!':
				case '?':
					break;

				default:
					SourceLocation location = ctx.file().resolve(token.offset + count, 1);
					ctx.error(location, ParserDiagnostics.UNEXPECTED_CHARACTER_IN_TYPE_QUALIFIERS, c);
					break;
				}

				++count;
			}

			for(char c : new StringBuilder((String)token.value).reverse().toString().toCharArray())
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
			String name = (String)ParseTools.consume(ctx).value;
			return new AstTypeUserDefined(name);
		}

		ParseTools.unexpectedToken(ctx, TokenCategory.TYPE);
		return null;
	}

	private static AstTypeFunction parseFunction(ParseContext ctx)
	{
		List<AstType> params = parseParameters(ctx);
		Maybe<AstType> ret = Maybe.none();

		if(ParseTools.option(ctx, "=>", true))
			ret = Maybe.some(parse(ctx));

		return new AstTypeFunction(ret, params);
	}

	private static List<AstType> parseParameters(ParseContext ctx)
	{
		return ParseTools.parenthesized(ctx, () ->
		{
			List<AstType> params = new ArrayList<>();

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

		List<AstType> types = new ArrayList<>();
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
		TokenType type = ParseTools.consumeExpected(ctx, TokenCategory.TYPE).type;

		switch(type)
		{
		case TYPE_VOID:    return AstTypeBuiltin.VOID;
		case TYPE_BYTE:    return AstTypeBuiltin.BYTE;
		case TYPE_BOOL:    return AstTypeBuiltin.BOOL;
		case TYPE_INT8:    return AstTypeBuiltin.INT8;
		case TYPE_INT16:   return AstTypeBuiltin.INT16;
		case TYPE_INT32:   return AstTypeBuiltin.INT32;
		case TYPE_INT64:   return AstTypeBuiltin.INT64;
		case TYPE_INT:     return AstTypeBuiltin.INT;
		case TYPE_UINT8:   return AstTypeBuiltin.UINT8;
		case TYPE_UINT16:  return AstTypeBuiltin.UINT16;
		case TYPE_UINT32:  return AstTypeBuiltin.UINT32;
		case TYPE_UINT64:  return AstTypeBuiltin.UINT64;
		case TYPE_UINT:    return AstTypeBuiltin.UINT;
		case TYPE_FLOAT32: return AstTypeBuiltin.FLOAT32;
		case TYPE_FLOAT64: return AstTypeBuiltin.FLOAT64;
		default: throw new AssertionError("unreachable");
		}
	}
}
