package katana.op;

import java.util.HashMap;
import java.util.Map;

public class BuiltinOps
{
	public static final Map<String, Operator> PREFIX_OPS = new HashMap<>();
	public static final Map<String, Operator> INFIX_OPS = new HashMap<>();
	public static final Map<String, Operator> POSTFIX_OPS = new HashMap<>();

	static
	{
		PREFIX_OPS.put("&", Operator.prefix("&"));
		PREFIX_OPS.put("*", Operator.prefix("*"));
		INFIX_OPS.put("=", Operator.infix("=", Associativity.NONE, 0));
	}

	public static boolean isBuiltin(String symbol, Kind kind)
	{
		switch(kind)
		{
		case PREFIX:  return PREFIX_OPS .get(symbol) != null;
		case INFIX:   return INFIX_OPS  .get(symbol) != null;
		case POSTFIX: return POSTFIX_OPS.get(symbol) != null;
		default: break;
		}

		throw new AssertionError("unreachable");
	}
}
