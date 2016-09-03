package katana.backend.llvm;

import katana.Maybe;
import katana.backend.PlatformContext;
import katana.sema.BuiltinFunc;
import katana.sema.type.Builtin;

import java.util.HashMap;
import java.util.Map;

public abstract class PlatformContextLlvm implements PlatformContext
{
	private static final Map<String, BuiltinFunc> BUILTINS = new HashMap<>();

	private static void registerBinaryOp(String name, String boolInstr, String sintInstr, String uintInstr, String floatInstr, String ptrInstr, Builtin ret)
	{
		BUILTINS.put(name, new BinaryOp(name, Maybe.wrap(boolInstr), Maybe.wrap(sintInstr), Maybe.wrap(uintInstr), Maybe.wrap(floatInstr), Maybe.wrap(ptrInstr), Maybe.wrap(ret)));
	}

	static
	{
		registerBinaryOp("katana.eq",   "icmp eq", "icmp eq",  "icmp eq",  "fcmp oeq", "icmp eq", Builtin.BOOL);
		registerBinaryOp("katana.neq",  "icmp ne", "icmp ne",  "icmp ne",  "fcmp one", "icmp ne", Builtin.BOOL);
		registerBinaryOp("katana.lt",   null,      "icmp slt", "icmp ult", "fcmp olt", null,      Builtin.BOOL);
		registerBinaryOp("katana.lteq", null,      "icmp sle", "icmp ule", "fcmp ole", null,      Builtin.BOOL);
		registerBinaryOp("katana.gt",   null,      "icmp sgt", "icmp ugt", "fcmp ogt", null,      Builtin.BOOL);
		registerBinaryOp("katana.gteq", null,      "icmp sge", "icmp uge", "fcmp oge", null,      Builtin.BOOL);

		registerBinaryOp("katana.add", null, "add",  "add",  "fadd", null, null);
		registerBinaryOp("katana.sub", null, "sub",  "sub",  "fsub", null, null);
		registerBinaryOp("katana.mul", null, "mul",  "mul",  "fmul", null, null);
		registerBinaryOp("katana.div", null, "sdiv", "udiv", "fdiv", null, null);
		registerBinaryOp("katana.mod", null, "srem", "urem", "frem", null, null);

		registerBinaryOp("katana.shl",  null,  "shl",  "shl",  null, null, null);
		registerBinaryOp("katana.shrl", null,  "lshr", "lshr", null, null, null);
		registerBinaryOp("katana.shra", null,  "ashr", "ashr", null, null, null);
		registerBinaryOp("katana.and",  "and", "and",  "and",  null, null, null);
		registerBinaryOp("katana.or",   "or",  "or",   "or",   null, null, null);
		registerBinaryOp("katana.xor",  "xor", "xor",  "xor",  null, null, null);
	}

	@Override
	public Maybe<BuiltinFunc> findBuiltin(String name)
	{
		BuiltinFunc func = BUILTINS.get(name);
		return Maybe.wrap(func);
	}
}
