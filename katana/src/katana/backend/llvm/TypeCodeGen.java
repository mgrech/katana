package katana.backend.llvm;

import katana.backend.PlatformContext;
import katana.sema.Type;
import katana.sema.type.*;
import katana.visitor.IVisitor;

@SuppressWarnings("unused")
public class TypeCodeGen implements IVisitor
{
	private TypeCodeGen() {}

	private String visit(Builtin type, PlatformContext context)
	{
		switch(type.which)
		{
		case INT8:
		case UINT8:
		case INT16:
		case UINT16:
		case INT32:
		case UINT32:
		case INT64:
		case UINT64:
		case INT:
		case UINT:
		case PINT:
		case UPINT:
			return "i" + 8 * type.sizeof(context);

		case BOOL:    return "i1";
		case FLOAT32: return "float";
		case FLOAT64: return "double";
		case PTR:     return "i8*";

		default: break;
		}

		throw new RuntimeException("unreachable");
	}

	private String visit(Opaque type, PlatformContext context)
	{
		return String.format("[%s x i8]", type.sizeof(context));
	}

	private String visit(Function type, PlatformContext context)
	{
		String ret = type.ret.isSome() ? TypeCodeGen.apply(type.ret.get(), context) : "void";

		StringBuilder params = new StringBuilder();

		if(!type.params.isEmpty())
		{
			params.append(TypeCodeGen.apply(type.params.get(0), context));

			for(int i = 1; i != type.params.size(); ++i)
			{
				params.append(", ");
				params.append(TypeCodeGen.apply(type.params.get(i), context));
			}
		}

		return String.format("%s(%s)", ret, params.toString());
	}

	private String visit(UserDefined type, PlatformContext context)
	{
		return '%' + type.data.qualifiedName().toString();
	}

	private String visit(Array type, PlatformContext context)
	{
		return String.format("[%s x %s]", type.sizeof(context), TypeCodeGen.apply(type.type, context));
	}

	public static String apply(Type type, PlatformContext context)
	{
		TypeCodeGen visitor = new TypeCodeGen();
		return (String)type.accept(visitor, context);
	}
}
