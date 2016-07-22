package katana.parser;

import katana.ast.Type;
import katana.ast.type.*;
import katana.scanner.Scanner;
import katana.scanner.Token;

import java.util.ArrayList;
import java.util.Optional;

public class TypeParser
{
	public static Type parse(Scanner scanner)
	{
		if(ParserTools.option(scanner, Token.Type.DECL_FN, true))
			return parseFunction(scanner);

		if(ParserTools.option(scanner, Token.Type.PUNCT_LBRACKET, true))
			return parseArray(scanner);

		if(ParserTools.option(scanner, Token.Type.TYPE_PTR, true))
			return new Pointer(TypeParser.parse(scanner));

		if(ParserTools.option(scanner, Token.Type.TYPE_OPAQUE, true))
			return parseOpaque(scanner);

		if(ParserTools.option(scanner, Token.Category.TYPE, false))
			return parseBuiltin(scanner);

		ParserTools.unexpectedToken(scanner, Token.Category.TYPE);
		throw new AssertionError("unreachable");
	}

	private static Function parseFunction(Scanner scanner)
	{
		ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);

		ArrayList<Type> params = new ArrayList<>();

		if(!ParserTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
		{
			params.add(parse(scanner));

			while(!ParserTools.option(scanner, Token.Type.PUNCT_RPAREN, false))
				params.add(parse(scanner));
		}

		ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);

		Optional<Type> ret = Optional.empty();

		if(ParserTools.option(scanner, Token.Type.PUNCT_RET, true))
			ret = Optional.of(parse(scanner));

		return new Function(ret, params);
	}

	private static Array parseArray(Scanner scanner)
	{
		String size = ParserTools.expectAndConsume(scanner, Token.Type.LIT_INT).value;
		ParserTools.expect(scanner, Token.Type.PUNCT_RBRACKET, true);
		return new Array(Integer.parseInt(size), TypeParser.parse(scanner));
	}

	private static Opaque parseOpaque(Scanner scanner)
	{
		ParserTools.expect(scanner, Token.Type.PUNCT_LPAREN, true);
		String size = ParserTools.expectAndConsume(scanner, Token.Type.LIT_INT).value;
		ParserTools.expect(scanner, Token.Type.PUNCT_COMMA, true);
		String alignment = ParserTools.expectAndConsume(scanner, Token.Type.LIT_INT).value;
		ParserTools.expect(scanner, Token.Type.PUNCT_RPAREN, true);
		return new Opaque(Integer.parseInt(size), Integer.parseInt(alignment));
	}

	private static Builtin parseBuiltin(Scanner scanner)
	{
		Token.Type type = ParserTools.expectAndConsume(scanner, Token.Category.TYPE).type;

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

		default: throw new AssertionError("unreachable");
		}
	}
}
