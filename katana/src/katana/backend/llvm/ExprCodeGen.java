package katana.backend.llvm;

import katana.Maybe;
import katana.backend.PlatformContext;
import katana.sema.Expr;
import katana.sema.Type;
import katana.sema.decl.Data;
import katana.sema.expr.*;
import katana.sema.type.Function;
import katana.sema.type.UserDefined;
import katana.visitor.IVisitor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ExprCodeGen implements IVisitor
{
	private ExprCodeGen() {}

	private String generateLoad(String where, String type, StringBuilder builder, FunctionContext fcontext)
	{
		String ssa = fcontext.allocateSSA();
		builder.append(String.format("\t%s = load %s, %s* %s\n", ssa, type, type, where));
		return ssa;
	}

	private void generateStore(String where, String what, String type, StringBuilder builder)
	{
		builder.append(String.format("\tstore %s %s, %s* %s\n", type, what, type, where));
	}

	private Maybe<String> visit(Addressof addressof, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return apply(addressof.expr, builder, context, fcontext);
	}

	private Maybe<String> visit(Alignof alignof, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some("" + alignof.type.alignof(context));
	}

	private String generateArrayAccess(boolean usedAsLValue, String arraySSA, Type fieldType, Expr index, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		Type indexType = index.type().unwrap();
		String indexTypeString = TypeCodeGen.apply(indexType, context);
		String indexSSA = apply(index, builder, context, fcontext).unwrap();
		String fieldTypeString = TypeCodeGen.apply(fieldType, context);

		String elemSSA = fcontext.allocateSSA();
		builder.append(String.format("\t%s = getelementptr %s, %s* %s, %s %s\n", elemSSA, fieldTypeString, fieldTypeString, arraySSA, indexTypeString, indexSSA));

		if(usedAsLValue)
			return elemSSA;

		return generateLoad(elemSSA, fieldTypeString, builder, fcontext);
	}

	private Maybe<String> visit(ArrayAccessLValue arrayAccess, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		boolean isUsedAsLValue = arrayAccess.isUsedAsLValue();
		arrayAccess.useAsLValue(true);
		String arraySSA = apply(arrayAccess.value, builder, context, fcontext).unwrap();
		arrayAccess.useAsLValue(isUsedAsLValue);
		Type fieldType = arrayAccess.type().unwrap();
		return Maybe.some(generateArrayAccess(isUsedAsLValue, arraySSA, fieldType, arrayAccess.index, builder, context, fcontext));
	}

	private Maybe<String> visit(ArrayAccessRValue arrayAccess, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		String arraySSA = apply(arrayAccess.expr, builder, context, fcontext).unwrap();
		Type arrayType = arrayAccess.expr.type().unwrap();
		String arrayTypeString = TypeCodeGen.apply(arrayType, context);
		int arrayAlignment = arrayType.alignof(context);

		String tmpSSA = fcontext.allocateSSA();
		builder.append(String.format("\t%s = alloca %s, align %s\n", tmpSSA, arrayTypeString, arrayAlignment));
		generateStore(tmpSSA, arraySSA, arrayTypeString, builder);

		Type fieldType = arrayAccess.type().unwrap();
		Type indexType = arrayAccess.index.type().unwrap();
		return Maybe.some(generateArrayAccess(false, tmpSSA, fieldType, arrayAccess.index, builder, context, fcontext));
	}

	private Maybe<String> visit(Assign assign, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		Type type = assign.right.type().unwrap();
		String typeString = TypeCodeGen.apply(assign.right.type().unwrap(), context);
		String right = apply(assign.right, builder, context, fcontext).unwrap();
		String left = apply(assign.left, builder, context, fcontext).unwrap();
		generateStore(left, right, typeString, builder);
		return Maybe.some(left);
	}

	private Maybe<String> visit(BuiltinCall builtinCall, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some(BuiltinCodeGen.apply(builtinCall, builder, context, fcontext));
	}

	private Maybe<String> visit(Deref deref, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		String ptrSSA = apply(deref.expr, builder, context, fcontext).unwrap();
		String castedPtrSSA = fcontext.allocateSSA();
		String typeString = TypeCodeGen.apply(deref.type, context);
		builder.append(String.format("\t%s = bitcast i8* %s to %s*\n", castedPtrSSA, ptrSSA, typeString));

		if(deref.isUsedAsLValue() || deref.type instanceof Function)
			return Maybe.some(castedPtrSSA);

		return Maybe.some(generateLoad(castedPtrSSA, typeString, builder, fcontext));
	}

	private Maybe<String> generateFunctionCall(String functionSSA, List<Expr> args, Maybe<Type> ret, Maybe<Boolean> inline, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		List<String> argSSAs = new ArrayList<>();

		for(Expr arg : args)
			argSSAs.add(apply(arg, builder, context, fcontext).unwrap());

		builder.append('\t');

		Maybe<String> retSSA = Maybe.none();

		if(ret.isSome())
		{
			retSSA = Maybe.some(fcontext.allocateSSA());
			builder.append(String.format("%s = ", retSSA.unwrap()));
		}

		String retTypeString = ret.map((r) -> TypeCodeGen.apply(r, context)).or("void");

		builder.append(String.format("call %s %s(", retTypeString, functionSSA));

		if(!args.isEmpty())
		{
			builder.append(TypeCodeGen.apply(args.get(0).type().unwrap(), context));
			builder.append(' ');
			builder.append(argSSAs.get(0));

			for(int i = 1; i != argSSAs.size(); ++i)
			{
				String argTypeString = TypeCodeGen.apply(args.get(i).type().unwrap(), context);
				builder.append(String.format(", %s %s", argTypeString, argSSAs.get(i)));
			}
		}

		builder.append(')');

		if(inline.isSome())
			if(inline.unwrap())
				builder.append(" alwaysinline");
			else
				builder.append(" noinline");

		builder.append('\n');

		return retSSA;
	}

	private Maybe<String> visit(DirectFunctionCall functionCall, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		String functionSSA = '@' + functionCall.function.qualifiedName().toString();
		return generateFunctionCall(functionSSA, functionCall.args, functionCall.function.ret, functionCall.inline, builder, context, fcontext);
	}

	private Maybe<String> visit(IndirectFunctionCall functionCall, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		String functionSSA = apply(functionCall.expr, builder, context, fcontext).unwrap();
		return generateFunctionCall(functionSSA, functionCall.args, functionCall.type(), Maybe.none(), builder, context, fcontext);
	}

	private Maybe<String> visit(FieldAccessLValue fieldAccess, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		boolean usedAsLValue = fieldAccess.isUsedAsLValue();
		fieldAccess.useAsLValue(true);
		String objectSSA = apply(fieldAccess.expr, builder, context, fcontext).unwrap();
		fieldAccess.useAsLValue(usedAsLValue);
		String fieldPtrSSA = fcontext.allocateSSA();
		Type fieldType = fieldAccess.type().unwrap();
		String fieldTypeString = TypeCodeGen.apply(fieldType, context);
		int fieldIndex = fieldAccess.field.index;
		builder.append(String.format("\t%s = getelementptr %s, %s* %s, i32 %s\n", fieldPtrSSA, fieldTypeString, fieldTypeString, objectSSA, fieldIndex));

		if(fieldAccess.isUsedAsLValue())
			return Maybe.some(fieldPtrSSA);

		return Maybe.some(generateLoad(fieldPtrSSA, fieldTypeString, builder, fcontext));
	}

	private Maybe<String> visit(FieldAccessRValue fieldAccess, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		String objectSSA = apply(fieldAccess.expr, builder, context, fcontext).unwrap();
		String fieldSSA = fcontext.allocateSSA();
		Type objectType = fieldAccess.expr.type().unwrap();
		String objectTypeString = TypeCodeGen.apply(objectType, context);
		int fieldIndex = fieldAccess.field.index;
		builder.append(String.format("\t%s = extractvalue %s %s, %s\n", fieldSSA, objectTypeString, objectSSA, fieldIndex));
		return Maybe.some(fieldSSA);
	}

	private Maybe<String> visit(LitBool litBool, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some("" + litBool.value);
	}

	private Maybe<String> visit(LitFloat litFloat, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		throw new RuntimeException("codegen for floating point literals not yet implemented");
	}

	private Maybe<String> visit(LitInt litInt, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some(litInt.value.toString());
	}

	private Maybe<String> visit(LitNull litNull, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some("null");
	}

	private byte[] utf8Encode(int cp)
	{
		StringBuilder builder = new StringBuilder();
		builder.appendCodePoint(cp);
		return builder.toString().getBytes(StandardCharsets.UTF_8);
	}

	private String escapeCharacter(int cp)
	{
		StringBuilder result = new StringBuilder();

		for(byte b : utf8Encode(cp))
			result.append(String.format("\\%02X", b));

		return result.toString();
	}

	private String escape(String s)
	{
		StringBuilder result = new StringBuilder();

		for(int cp : s.codePoints().toArray())
			if(cp == '"' || cp == '\\' || Character.isISOControl(cp))
				result.append(escapeCharacter(cp));
			else
				result.appendCodePoint(cp);

		return result.toString();
	}

	private Maybe<String> visit(LitString litString, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some("c\"" + escape(litString.value) + '"');
	}

	private Maybe<String> visit(NamedFunc namedFunc, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some("@" + namedFunc.func.qualifiedName());
	}

	private String visitNamedValue(char prefix, boolean usedAsLValue, Type type, String name, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		if(usedAsLValue)
			return prefix + name;

		String typeString = TypeCodeGen.apply(type, context);
		return generateLoad(prefix + name, typeString, builder, fcontext);
	}

	private Maybe<String> visit(NamedGlobal namedGlobal, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some(visitNamedValue('@', namedGlobal.isUsedAsLValue(), namedGlobal.global.type, namedGlobal.global.name, builder, context, fcontext));
	}

	private Maybe<String> visit(NamedLocal namedLocal, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some(visitNamedValue('%', namedLocal.isUsedAsLValue(), namedLocal.local.type, namedLocal.local.name, builder, context, fcontext));
	}

	private Maybe<String> visit(NamedParam namedParam, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some(visitNamedValue('%', namedParam.isUsedAsLValue(), namedParam.param.type, namedParam.param.name, builder, context, fcontext));
	}

	private Maybe<String> visit(Offsetof offsetof, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		String offsetPtrSSA = fcontext.allocateSSA();
		Data.Field field = offsetof.field;
		String typeString = TypeCodeGen.apply(new UserDefined(field.data()), context);
		builder.append(String.format("\t%s = getelementptr %s null, i32 %s\n", offsetPtrSSA, typeString, field.index));

		String offsetSSA = fcontext.allocateSSA();
		String offsetTypeString = TypeCodeGen.apply(offsetof.type().unwrap(), context);
		builder.append(String.format("\t%s = ptrtoint %s* %s to %s\n", offsetSSA, typeString, offsetPtrSSA, offsetTypeString));
		return Maybe.some(offsetSSA);
	}

	private Maybe<String> visit(Sizeof sizeof, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		return Maybe.some("" + sizeof.type.sizeof(context));
	}

	public static Maybe<String> apply(Expr expr, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		ExprCodeGen visitor = new ExprCodeGen();
		return (Maybe<String>)expr.accept(visitor, builder, context, fcontext);
	}
}
