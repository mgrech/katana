package katana.backend.llvm;

import katana.BuiltinType;
import katana.Maybe;
import katana.backend.PlatformContext;
import katana.sema.BuiltinFunc;
import katana.sema.Type;
import katana.sema.expr.BuiltinCall;
import katana.sema.type.Builtin;

import java.util.List;

public class BinaryOp extends BuiltinFunc
{
	public BinaryOp(String name, Maybe<String> boolInstr, Maybe<String> sintInstr, Maybe<String> uintInstr, Maybe<String> floatInstr, Maybe<String> ptrInstr, Maybe<Type> ret)
	{
		super(name);
		this.boolInstr = boolInstr;
		this.sintInstr = sintInstr;
		this.uintInstr = uintInstr;
		this.floatInstr = floatInstr;
		this.ptrInstr = ptrInstr;
		this.ret = ret;
	}

	private void unsupportedType(String what)
	{
		throw new RuntimeException(String.format("builtin %s does not support %s", name, what));
	}

	private void checkTypeSupport(BuiltinType type)
	{
		switch(type.kind)
		{
		case BOOL:
			if(boolInstr.isNone())
				unsupportedType("bools");
			break;

		case INT:
			if(sintInstr.isNone())
				unsupportedType("signed integer types");
			break;

		case UINT:
			if(uintInstr.isNone())
				unsupportedType("unsigned integer types");
			break;

		case FLOAT:
			if(floatInstr.isNone())
				unsupportedType("floating point types");
			break;

		case PTR:
			if(ptrInstr.isNone())
				unsupportedType("pointers");
			break;

		default: throw new AssertionError("unreachable");
		}
	}

	@Override
	public Maybe<Type> validateCall(List<Type> args)
	{
		if(args.size() != 2)
			throw new RuntimeException(String.format("builtin %s expects 2 arguments", name));

		if(!Type.same(args.get(0), args.get(1)))
			throw new RuntimeException(String.format("arguments to builtin %s must be of same type", name));

		Type type = ret.or(args.get(0));

		if(!(type instanceof Builtin))
			throw new RuntimeException(String.format("builtin %s requires built-in type", name));

		Builtin builtin = (Builtin)type;
		checkTypeSupport(builtin.which);
		return Maybe.some(type);
	}

	private String instrForType(Builtin type)
	{
		switch(type.which.kind)
		{
		case BOOL: return boolInstr.unwrap();
		case INT: return sintInstr.unwrap();
		case UINT: return uintInstr.unwrap();
		case FLOAT: return floatInstr.unwrap();
		case PTR: return ptrInstr.unwrap();
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	@Override
	public Maybe<String> generateCall(BuiltinCall call, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		Builtin type = (Builtin)call.args.get(0).type().unwrap();
		String typeString = TypeCodeGen.apply(type, context);
		String leftSSA = ExprCodeGen.apply(call.args.get(0), builder, context, fcontext).unwrap();
		String rightSSA = ExprCodeGen.apply(call.args.get(1), builder, context, fcontext).unwrap();
		String instr = instrForType(type);
		String resultSSA = fcontext.allocateSSA();
		builder.append(String.format("\t%s = %s %s %s, %s\n", resultSSA, instr, typeString, leftSSA, rightSSA));
		return Maybe.some(resultSSA);
	}

	private Maybe<String> boolInstr;
	private Maybe<String> sintInstr;
	private Maybe<String> uintInstr;
	private Maybe<String> floatInstr;
	private Maybe<String> ptrInstr;
	private Maybe<Type> ret;
}
