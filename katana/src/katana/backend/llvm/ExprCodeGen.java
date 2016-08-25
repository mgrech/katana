package katana.backend.llvm;

import katana.backend.PlatformContext;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.expr.*;
import katana.visitor.IVisitor;

@SuppressWarnings("unused")
public class ExprCodeGen implements IVisitor
{
	private ExprCodeGen() {}

	private String visit(Addressof addressof, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return apply(addressof.expr, builder, context, fcontext);
	}

	private String visit(Alignof alignof, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return "" + alignof.type.alignof(context);
	}

	private String visit(ArrayAccess arrayAccess, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		throw new RuntimeException("array access codegen nyi");
	}

	private String visit(Assign assign, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		Type type = assign.right.type().unwrap();
		String typeString = TypeCodeGen.apply(assign.right.type().unwrap(), context);
		String right = apply(assign.right, builder, context, fcontext);
		String left = apply(assign.left, builder, context, fcontext);
		builder.append(String.format("\tstore %s %s, %s* %s\n", typeString, right, typeString, left));
		return left;
	}

	private String visit(BuiltinCall builtinCall, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return BuiltinCodeGen.apply(builtinCall, builder, context, fcontext);
	}

	private String visit(Deref deref, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		throw new RuntimeException("deref codegen nyi");
	}

	private String visit(DirectFunctionCall functionCall, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		throw new RuntimeException("direct function call codegen nyi");
	}

	private String visit(FieldAccess fieldAccess, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		if(!(fieldAccess.expr instanceof LValueExpr))
			throw new RuntimeException("field access on rvalue nyi");

		String obj = ExprCodeGen.apply(fieldAccess.expr, builder, context, fcontext);

		return "%" + fcontext.nextTemporary();
	}

	private String visit(IndirectFunctionCall functionCall, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		throw new RuntimeException("indirect function call codegen nyi");
	}

	private String visit(LitBool litBool, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return "" + litBool.value;
	}

	private String visit(LitFloat litFloat, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		throw new RuntimeException("floatlit codegen nyi");
	}

	private String visit(LitInt litInt, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return litInt.value.toString();
	}

	private String visit(LitNull litNull, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return "null";
	}

	private String visit(LitString litString, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		throw new RuntimeException("stringlit codegen nyi");
	}

	private String visit(NamedFunc namedFunc, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return "@" + namedFunc.func.qualifiedName();
	}

	private String visitNamedValue(char prefix, boolean usedAsLValue, Type type, String name, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		if(usedAsLValue)
			return prefix + name;

		int tmp = fcontext.nextTemporary();
		String typeString = TypeCodeGen.apply(type, context);
		builder.append(String.format("\t%%%s = load %s, %s* %s%s\n", tmp, typeString, typeString, prefix, name));
		return "%" + tmp;
	}

	private String visit(NamedGlobal namedGlobal, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return visitNamedValue('@', namedGlobal.usedAsLValue, namedGlobal.global.type, namedGlobal.global.name, builder, context, fcontext);
	}

	private String visit(NamedLocal namedLocal, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return visitNamedValue('%', namedLocal.usedAsLValue, namedLocal.local.type, namedLocal.local.name, builder, context, fcontext);
	}

	private String visit(NamedParam namedParam, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return visitNamedValue('%', namedParam.usedAsLValue, namedParam.param.type, namedParam.param.name, builder, context, fcontext);
	}

	private String visit(Offsetof offsetof, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		throw new RuntimeException("offsetof codegen nyi");
	}

	private String visit(Sizeof sizeof, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return "" + sizeof.type.sizeof(context);
	}

	public static String apply(Expr expr, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		ExprCodeGen visitor = new ExprCodeGen();
		return (String)expr.accept(visitor, builder, context, fcontext);
	}
}
