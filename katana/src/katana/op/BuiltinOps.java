package katana.op;

import katana.sema.decl.SemaDeclOperator;
import katana.utils.Maybe;

import java.util.HashMap;
import java.util.Map;

public class BuiltinOps
{
	public static final Map<String, SemaDeclOperator> PREFIX_OPS = new HashMap<>();
	public static final Map<String, SemaDeclOperator> INFIX_OPS = new HashMap<>();
	public static final Map<String, SemaDeclOperator> POSTFIX_OPS = new HashMap<>();

	private static void registerBuiltinOp(Operator op)
	{
		switch(op.kind)
		{
		case PREFIX:  PREFIX_OPS .put(op.symbol, new SemaDeclOperator(null, true, Operator.prefix(op.symbol))); break;
		case INFIX:   INFIX_OPS  .put(op.symbol, new SemaDeclOperator(null, true, Operator.infix(op.symbol, op.associativity, op.precedence))); break;
		case POSTFIX: POSTFIX_OPS.put(op.symbol, new SemaDeclOperator(null, true, Operator.postfix(op.symbol))); break;
		default: throw new AssertionError("unreachable");
		}
	}

	static
	{
		registerBuiltinOp(Operator.prefix("&"));
		registerBuiltinOp(Operator.prefix("*"));
		registerBuiltinOp(Operator.infix("=", Associativity.NONE, 0));
	}

	public static Maybe<SemaDeclOperator> find(String symbol, Kind kind)
	{
		switch(kind)
		{
		case PREFIX:  return Maybe.wrap(PREFIX_OPS .get(symbol));
		case INFIX:   return Maybe.wrap(INFIX_OPS  .get(symbol));
		case POSTFIX: return Maybe.wrap(POSTFIX_OPS.get(symbol));
		default: break;
		}

		throw new AssertionError("unreachable");
	}
}
