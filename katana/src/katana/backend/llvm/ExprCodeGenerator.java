// Copyright 2016 Markus Grech
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

package katana.backend.llvm;

import katana.BuiltinType;
import katana.analysis.TypeHelper;
import katana.backend.PlatformContext;
import katana.sema.decl.SemaDeclData;
import katana.sema.decl.SemaDeclExternFunction;
import katana.sema.expr.*;
import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeFunction;
import katana.sema.type.SemaTypeUserDefined;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ExprCodeGenerator implements IVisitor
{
	private StringBuilder builder;
	private PlatformContext context;
	private FunctionContext fcontext;

	private ExprCodeGenerator(StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		this.builder = builder;
		this.context = context;
		this.fcontext = fcontext;
	}

	public static Maybe<String> generate(SemaExpr expr, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		ExprCodeGenerator visitor = new ExprCodeGenerator(builder, context, fcontext);
		return (Maybe<String>)expr.accept(visitor);
	}

	private String generateLoad(String where, String type)
	{
		String ssa = fcontext.allocateSSA();
		builder.append(String.format("\t%s = load %s, %s* %s\n", ssa, type, type, where));
		return ssa;
	}

	private void generateStore(String where, String what, String type)
	{
		builder.append(String.format("\tstore %s %s, %s* %s\n", type, what, type, where));
	}

	private Maybe<String> visit(SemaExprAddressof addressof)
	{
		return Maybe.some(generate(addressof.expr, builder, context, fcontext).unwrap());
	}

	private Maybe<String> visit(SemaExprAlignof alignof)
	{
		return Maybe.some("" + alignof.type.alignof(context));
	}

	private String generateArrayAccess(boolean usedAsLValue, String arraySSA, SemaType arrayType, SemaType fieldType, SemaExpr index)
	{
		String indexTypeString = TypeCodeGenerator.generate(index.type(), context);
		String indexSSA = generate(index, builder, context, fcontext).unwrap();
		String fieldTypeString = TypeCodeGenerator.generate(fieldType, context);
		String arrayTypeString = TypeCodeGenerator.generate(arrayType, context);
		String elemSSA = fcontext.allocateSSA();
		builder.append(String.format("\t%s = getelementptr %s, %s* %s, i64 0, %s %s\n", elemSSA, arrayTypeString, arrayTypeString, arraySSA, indexTypeString, indexSSA));

		if(usedAsLValue)
			return elemSSA;

		return generateLoad(elemSSA, fieldTypeString);
	}

	private Maybe<String> visit(SemaExprArrayAccessLValue arrayAccess)
	{
		boolean isUsedAsLValue = arrayAccess.isUsedAsLValue();
		arrayAccess.useAsLValue(true);
		String arraySSA = generate(arrayAccess.value, builder, context, fcontext).unwrap();
		arrayAccess.useAsLValue(isUsedAsLValue);
		SemaType fieldType = arrayAccess.type();
		return Maybe.some(generateArrayAccess(isUsedAsLValue, arraySSA, arrayAccess.value.type(), fieldType, arrayAccess.index));
	}

	private Maybe<String> visit(SemaExprArrayAccessRValue arrayAccess)
	{
		String arraySSA = generate(arrayAccess.expr, builder, context, fcontext).unwrap();
		SemaType arrayType = arrayAccess.expr.type();
		String arrayTypeString = TypeCodeGenerator.generate(arrayType, context);
		BigInteger arrayAlignment = arrayType.alignof(context);

		String tmpSSA = fcontext.allocateSSA();
		builder.append(String.format("\t%s = alloca %s, align %s\n", tmpSSA, arrayTypeString, arrayAlignment));
		generateStore(tmpSSA, arraySSA, arrayTypeString);

		SemaType fieldType = arrayAccess.type();
		SemaType indexType = arrayAccess.index.type();
		return Maybe.some(generateArrayAccess(false, tmpSSA, arrayAccess.expr.type(), fieldType, arrayAccess.index));
	}

	private Maybe<String> visit(SemaExprAssign assign)
	{
		SemaType type = assign.right.type();
		String typeString = TypeCodeGenerator.generate(assign.right.type(), context);
		String right = generate(assign.right, builder, context, fcontext).unwrap();
		String left = generate(assign.left, builder, context, fcontext).unwrap();
		generateStore(left, right, typeString);
		return Maybe.some(left);
	}

	private Maybe<String> visit(SemaExprBuiltinCall builtinCall)
	{
		return builtinCall.func.generateCall(builtinCall, builder, context, fcontext);
	}

	private String instrForCast(SemaType targetType, SemaExprCast.Kind kind)
	{
		switch(kind)
		{
		case WIDEN_CAST:
			if(TypeHelper.isFloatingPointType(targetType))
				return "fpext";

			if(TypeHelper.isSigned(targetType))
				return "sext";

			if(TypeHelper.isUnsigned(targetType))
				return "zext";

			throw new AssertionError("unreachable");

		case NARROW_CAST:
			if(TypeHelper.isFloatingPointType(targetType))
				return "fptrunc";

			if(TypeHelper.isIntegerType(targetType))
				return "trunc";

			throw new AssertionError("unreachable");

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private String generateCast(String valueSSA, SemaType sourceType, SemaType targetType, SemaExprCast.Kind kind)
	{
		String resultSSA = fcontext.allocateSSA();
		String sourceTypeString = TypeCodeGenerator.generate(sourceType, context);
		String targetTypeString = TypeCodeGenerator.generate(targetType, context);

		String instr = instrForCast(targetType, kind);
		builder.append(String.format("\t%s = %s %s %s to %s\n", resultSSA, instr, sourceTypeString, valueSSA, targetTypeString));
		return resultSSA;
	}

	private Maybe<String> visit(SemaExprCast cast)
	{
		String valueSSA = generate(cast.expr, builder, context, fcontext).unwrap();
		String resultSSA;

		SemaType sourceType = cast.expr.type();
		SemaType targetType = cast.type;

		switch(cast.kind)
		{
		case SIGN_CAST:
			resultSSA = valueSSA;
			break;

		case WIDEN_CAST:
			if(sourceType.sizeof(context).equals(targetType.sizeof(context)))
			{
				resultSSA = valueSSA;
				break;
			}

			resultSSA = generateCast(valueSSA, sourceType, targetType, SemaExprCast.Kind.WIDEN_CAST);
			break;

		case NARROW_CAST:
			if(sourceType.sizeof(context).equals(targetType.sizeof(context)))
			{
				resultSSA = valueSSA;
				break;
			}

			resultSSA = generateCast(valueSSA, sourceType, targetType, SemaExprCast.Kind.NARROW_CAST);
			break;

		default: throw new AssertionError("unreachable");
		}

		return Maybe.some(resultSSA);
	}

	private Maybe<String> visit(SemaExprConstLValue const_)
	{
		return generate(const_.expr, builder, context, fcontext);
	}

	private Maybe<String> visit(SemaExprConstRValue const_)
	{
		return generate(const_.expr, builder, context, fcontext);
	}

	private Maybe<String> visit(SemaExprDeref deref)
	{
		String ptrSSA = generate(deref.expr, builder, context, fcontext).unwrap();

		if(deref.isUsedAsLValue() || deref.type() instanceof SemaTypeFunction)
			return Maybe.some(ptrSSA);

		String typeString = TypeCodeGenerator.generate(deref.type(), context);
		return Maybe.some(generateLoad(ptrSSA, typeString));
	}

	private Maybe<String> generateFunctionCall(String functionSSA, List<SemaExpr> args, SemaType ret, Maybe<Boolean> inline)
	{
		List<String> argSSAs = new ArrayList<>();

		for(SemaExpr arg : args)
			argSSAs.add(generate(arg, builder, context, fcontext).unwrap());

		builder.append('\t');

		Maybe<String> retSSA = Maybe.none();

		if(!TypeHelper.isVoidType(ret))
		{
			retSSA = Maybe.some(fcontext.allocateSSA());
			builder.append(String.format("%s = ", retSSA.unwrap()));
		}

		String retTypeString = TypeCodeGenerator.generate(ret, context);

		builder.append(String.format("call %s %s(", retTypeString, functionSSA));

		if(!args.isEmpty())
		{
			builder.append(TypeCodeGenerator.generate(args.get(0).type(), context));
			builder.append(' ');
			builder.append(argSSAs.get(0));

			for(int i = 1; i != argSSAs.size(); ++i)
			{
				String argTypeString = TypeCodeGenerator.generate(args.get(i).type(), context);
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

	private Maybe<String> visit(SemaExprDirectFunctionCall functionCall)
	{
		String name;

		if(functionCall.function instanceof SemaDeclExternFunction)
		{
			SemaDeclExternFunction extfn = (SemaDeclExternFunction)functionCall.function;
			name = extfn.externName;
		}

		else
			name = FunctionNameMangler.mangle(functionCall.function);

		String functionSSA = '@' + name;
		return generateFunctionCall(functionSSA, functionCall.args, functionCall.function.ret, functionCall.inline);
	}

	private Maybe<String> visit(SemaExprIndirectFunctionCall functionCall)
	{
		String functionSSA = generate(functionCall.expr, builder, context, fcontext).unwrap();
		return generateFunctionCall(functionSSA, functionCall.args, functionCall.type(), Maybe.none());
	}

	private Maybe<String> visit(SemaExprFieldAccessLValue fieldAccess)
	{
		boolean usedAsLValue = fieldAccess.isUsedAsLValue();
		fieldAccess.useAsLValue(true);
		String objectSSA = generate(fieldAccess.expr, builder, context, fcontext).unwrap();
		fieldAccess.useAsLValue(usedAsLValue);
		String fieldPtrSSA = fcontext.allocateSSA();
		SemaType fieldType = fieldAccess.type();
		String fieldTypeString = TypeCodeGenerator.generate(fieldType, context);
		int fieldIndex = fieldAccess.field.index;
		String objectTypeString = TypeCodeGenerator.generate(fieldAccess.expr.type(), context);
		builder.append(String.format("\t%s = getelementptr %s, %s* %s, i32 0, i32 %s\n", fieldPtrSSA, objectTypeString, objectTypeString, objectSSA, fieldIndex));

		if(fieldAccess.isUsedAsLValue())
			return Maybe.some(fieldPtrSSA);

		return Maybe.some(generateLoad(fieldPtrSSA, fieldTypeString));
	}

	private Maybe<String> visit(SemaExprFieldAccessRValue fieldAccess)
	{
		String objectSSA = generate(fieldAccess.expr, builder, context, fcontext).unwrap();
		String fieldSSA = fcontext.allocateSSA();
		SemaType objectType = fieldAccess.expr.type();
		String objectTypeString = TypeCodeGenerator.generate(objectType, context);
		int fieldIndex = fieldAccess.field.index;
		builder.append(String.format("\t%s = extractvalue %s %s, %s\n", fieldSSA, objectTypeString, objectSSA, fieldIndex));
		return Maybe.some(fieldSSA);
	}

	private Maybe<String> visit(SemaExprLitArray lit)
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');

		if(!lit.values.isEmpty())
		{
			builder.append(TypeCodeGenerator.generate(lit.values.get(0).type(), context));
			builder.append(' ');
			builder.append(generate(lit.values.get(0), builder, context, fcontext).unwrap());

			for(int i = 1; i != lit.values.size(); ++i)
			{
				SemaExpr expr = lit.values.get(i);
				builder.append(", ");
				builder.append(TypeCodeGenerator.generate(expr.type(), context));
				builder.append(' ');
				builder.append(generate(expr, builder, context, fcontext).unwrap());
			}
		}

		builder.append(']');
		return Maybe.some(builder.toString());
	}

	private Maybe<String> visit(SemaExprLitBool lit)
	{
		return Maybe.some("" + lit.value);
	}

	private String toFloatHexString(BigDecimal bd)
	{
		// llvm requires float literals to be as wide as double literals but representable in a float
		// hence we take the float value and widen it to double
		double d = (double)bd.floatValue();
		long l = Double.doubleToLongBits(d);
		return String.format("0x%x", l);
	}

	private String toDoubleHexString(BigDecimal bd)
	{
		double d = bd.doubleValue();
		long l = Double.doubleToRawLongBits(d);
		return String.format("0x%x", l);
	}

	private Maybe<String> visit(SemaExprLitFloat lit)
	{
		String s = lit.type == BuiltinType.FLOAT32
			? toFloatHexString(lit.value)
			: toDoubleHexString(lit.value);

		return Maybe.some(s);
	}

	private Maybe<String> visit(SemaExprLitInt lit)
	{
		return Maybe.some(lit.value.toString());
	}

	private Maybe<String> visit(SemaExprLitNull lit)
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

	private Maybe<String> visit(SemaExprLitString lit)
	{
		return Maybe.some("c\"" + escape(lit.value) + '"');
	}

	private Maybe<String> visit(SemaExprNamedFunc namedFunc)
	{
		if(namedFunc.func instanceof SemaDeclExternFunction)
			return Maybe.some("@" + ((SemaDeclExternFunction)namedFunc.func).externName);

		return Maybe.some("@" + FunctionNameMangler.mangle(namedFunc.func));
	}

	private String visitNamedValue(char prefix, boolean usedAsLValue, SemaType type, String name)
	{
		if(usedAsLValue)
			return prefix + name;

		String typeString = TypeCodeGenerator.generate(type, context);
		return generateLoad(prefix + name, typeString);
	}

	private Maybe<String> visit(SemaExprNamedGlobal namedGlobal)
	{
		return Maybe.some(visitNamedValue('@', namedGlobal.isUsedAsLValue(), namedGlobal.global.type, namedGlobal.global.qualifiedName().toString()));
	}

	private Maybe<String> visit(SemaExprNamedLocal namedLocal)
	{
		return Maybe.some(visitNamedValue('%', namedLocal.isUsedAsLValue(), namedLocal.local.type, namedLocal.local.name));
	}

	private Maybe<String> visit(SemaExprNamedParam namedParam)
	{
		return Maybe.some(visitNamedValue('%', namedParam.isUsedAsLValue(), namedParam.param.type, namedParam.param.name));
	}

	private Maybe<String> visit(SemaExprOffsetof offsetof)
	{
		String offsetPtrSSA = fcontext.allocateSSA();
		SemaDeclData.Field field = offsetof.field;
		String typeString = TypeCodeGenerator.generate(new SemaTypeUserDefined(field.data()), context);
		builder.append(String.format("\t%s = getelementptr %s, %s* null, i32 %s\n", offsetPtrSSA, typeString, typeString, field.index));

		String offsetSSA = fcontext.allocateSSA();
		String offsetTypeString = TypeCodeGenerator.generate(offsetof.type(), context);
		builder.append(String.format("\t%s = ptrtoint %s* %s to %s\n", offsetSSA, typeString, offsetPtrSSA, offsetTypeString));
		return Maybe.some(offsetSSA);
	}

	private Maybe<String> visit(SemaExprSizeof sizeof)
	{
		return Maybe.some("" + sizeof.type.sizeof(context));
	}

	private Maybe<String> visit(SSAExpr ssa)
	{
		return Maybe.some(ssa.name);
	}
}
