// Copyright 2016-2017 Markus Grech
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

package io.katana.compiler.backend.llvm;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.decl.SemaDeclExternFunction;
import io.katana.compiler.sema.expr.*;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ExprCodeGenerator implements IVisitor
{
	private static final String ZEROSIZE_VALUE_ADDRESS = "inttoptr (i8 1 to i8*)";

	private FileCodegenContext context;
	private FunctionCodegenContext fcontext;

	private ExprCodeGenerator(FileCodegenContext context, FunctionCodegenContext fcontext)
	{
		this.context = context;
		this.fcontext = fcontext;
	}

	public static Maybe<String> generate(SemaExpr expr, FileCodegenContext context, FunctionCodegenContext fcontext)
	{
		ExprCodeGenerator visitor = new ExprCodeGenerator(context, fcontext);
		return (Maybe<String>)expr.accept(visitor);
	}

	private String generateLoad(String where, String type)
	{
		String ssa = fcontext.allocateSsa();
		context.writef("\t%s = load %s, %s* %s\n", ssa, type, type, where);
		return ssa;
	}

	private void generateStore(String where, String what, String type)
	{
		context.writef("\tstore %s %s, %s* %s\n", type, what, type, where);
	}

	private Maybe<String> visit(RValueToLValueConversion conversion)
	{
		String exprSsa = generate(conversion.expr, context, fcontext).unwrap();
		String exprType = TypeCodeGenerator.generate(conversion.expr.type(), context.platform());
		String resultSsa = fcontext.allocateSsa();
		generateStore(resultSsa, exprSsa, exprType);
		return Maybe.some(resultSsa);
	}

	private Maybe<String> visit(SemaExprAddressof addressof)
	{
		if(Types.isZeroSized(addressof.expr.type()))
			return Maybe.some(ZEROSIZE_VALUE_ADDRESS);

		return generate(addressof.expr, context, fcontext);
	}

	private Maybe<String> visit(SemaExprAlignof alignof)
	{
		return Maybe.some("" + Types.alignof(alignof.type, context.platform()));
	}

	private Maybe<String> visit(SemaExprArrayAccess arrayAccess)
	{
		if(Types.isZeroSized(arrayAccess.type()))
			return Maybe.none();

		boolean isRValue = arrayAccess.expr.kind() == ExprKind.RVALUE;

		if(isRValue)
			arrayAccess.expr = new RValueToLValueConversion(arrayAccess.expr);

		String arraySsa = generate(arrayAccess.expr, context, fcontext).unwrap();
		SemaType arrayType = arrayAccess.expr.type();
		String arrayTypeString = TypeCodeGenerator.generate(arrayType, context.platform());

		SemaType fieldType = arrayAccess.type();
		String fieldTypeString = TypeCodeGenerator.generate(fieldType, context.platform());
		SemaType indexType = arrayAccess.index.type();
		String indexTypeString = TypeCodeGenerator.generate(indexType, context.platform());

		String indexSsa = generate(arrayAccess.index, context, fcontext).unwrap();
		String fieldPtrSsa = fcontext.allocateSsa();
		context.writef("\t%s = getelementptr %s, %s* %s, i64 0, %s %s\n", fieldPtrSsa, arrayTypeString, arrayTypeString, arraySsa, indexTypeString, indexSsa);

		if(isRValue)
			return Maybe.some(generateLoad(fieldPtrSsa, fieldTypeString));

		return Maybe.some(fieldPtrSsa);
	}

	private Maybe<String> visit(SemaExprAssign assign)
	{
		if(Types.isZeroSized(assign.type()))
			return Maybe.none();

		SemaType type = assign.right.type();
		String typeString = TypeCodeGenerator.generate(assign.right.type(), context.platform());
		String right = generate(assign.right, context, fcontext).unwrap();
		String left = generate(assign.left, context, fcontext).unwrap();
		generateStore(left, right, typeString);
		return Maybe.some(left);
	}

	private Maybe<String> visit(SemaExprBuiltinCall builtinCall)
	{
		return builtinCall.func.generateCall(builtinCall, context, fcontext);
	}

	private String instrForCast(SemaType targetType, SemaExprCast.Kind kind)
	{
		switch(kind)
		{
		case WIDEN_CAST:
			if(Types.isFloatingPoint(targetType))
				return "fpext";

			if(Types.isSigned(targetType))
				return "sext";

			if(Types.isUnsigned(targetType))
				return "zext";

			throw new AssertionError("unreachable");

		case NARROW_CAST:
			if(Types.isFloatingPoint(targetType))
				return "fptrunc";

			if(Types.isInteger(targetType))
				return "trunc";

			throw new AssertionError("unreachable");

		case POINTER_CAST:
			if(Types.isPointer(targetType))
				return "inttoptr";

			return "ptrtoint";

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private String generateCast(String valueSsa, SemaType sourceType, SemaType targetType, SemaExprCast.Kind kind)
	{
		String resultSsa = fcontext.allocateSsa();
		String sourceTypeString = TypeCodeGenerator.generate(sourceType, context.platform());
		String targetTypeString = TypeCodeGenerator.generate(targetType, context.platform());

		String instr = instrForCast(targetType, kind);
		context.writef("\t%s = %s %s %s to %s\n", resultSsa, instr, sourceTypeString, valueSsa, targetTypeString);
		return resultSsa;
	}

	private Maybe<String> visit(SemaExprCast cast)
	{
		String valueSsa = generate(cast.expr, context, fcontext).unwrap();
		String resultSsa;

		SemaType sourceType = cast.expr.type();
		SemaType targetType = cast.type;

		switch(cast.kind)
		{
		case SIGN_CAST:
			resultSsa = valueSsa;
			break;

		case WIDEN_CAST:
			if(Types.equalSizes(sourceType, targetType, context.platform()))
			{
				resultSsa = valueSsa;
				break;
			}

			resultSsa = generateCast(valueSsa, sourceType, targetType, SemaExprCast.Kind.WIDEN_CAST);
			break;

		case NARROW_CAST:
			if(Types.equalSizes(sourceType, targetType, context.platform()))
			{
				resultSsa = valueSsa;
				break;
			}

			resultSsa = generateCast(valueSsa, sourceType, targetType, SemaExprCast.Kind.NARROW_CAST);
			break;

		case POINTER_CAST:
			if(Types.equal(Types.removeConst(sourceType), Types.removeConst(targetType)))
			{
				resultSsa = valueSsa;
				break;
			}

			resultSsa = generateCast(valueSsa, sourceType, targetType, SemaExprCast.Kind.POINTER_CAST);
			break;

		default: throw new AssertionError("unreachable");
		}

		return Maybe.some(resultSsa);
	}

	private Maybe<String> visit(SemaExprConst const_)
	{
		return generate(const_.expr, context, fcontext);
	}

	private Maybe<String> visit(SemaExprDeref deref)
	{
		if(Types.isZeroSized(deref.type()))
			return Maybe.none();

		return generate(deref.expr, context, fcontext);
	}

	private Maybe<String> generateFunctionCall(String functionSsa, List<SemaExpr> args, SemaType ret, Maybe<Boolean> inline)
	{
		List<String> argsSsa = new ArrayList<>();

		for(SemaExpr arg : args)
			argsSsa.add(generate(arg, context, fcontext).unwrap());

		context.write('\t');

		Maybe<String> retSsa = Maybe.none();

		if(!Types.isZeroSized(ret))
		{
			retSsa = Maybe.some(fcontext.allocateSsa());
			context.writef("%s = ", retSsa.unwrap());
		}

		String retTypeString = TypeCodeGenerator.generate(ret, context.platform());

		context.writef("call %s %s(", retTypeString, functionSsa);

		if(!args.isEmpty())
		{
			if(!Types.isZeroSized(args.get(0).type()))
			{
				context.write(TypeCodeGenerator.generate(args.get(0).type(), context.platform()));
				context.write(' ');
				context.write(argsSsa.get(0));
			}

			for(int i = 1; i != argsSsa.size(); ++i)
			{
				if(!Types.isZeroSized(args.get(i).type()))
				{
					String argTypeString = TypeCodeGenerator.generate(args.get(i).type(), context.platform());
					context.writef(", %s %s", argTypeString, argsSsa.get(i));
				}
			}
		}

		context.write(')');

		if(inline.isSome())
			if(inline.unwrap())
				context.write(" alwaysinline");
			else
				context.write(" noinline");

		context.write('\n');

		return retSsa;
	}

	private Maybe<String> visit(SemaExprDirectFunctionCall functionCall)
	{
		String name;

		if(functionCall.function instanceof SemaDeclExternFunction)
		{
			SemaDeclExternFunction extfn = (SemaDeclExternFunction)functionCall.function;
			name = extfn.externName.or(extfn.name());
		}

		else
			name = FunctionNameMangler.mangle(functionCall.function);

		String functionSsa = '@' + name;
		return generateFunctionCall(functionSsa, functionCall.args, functionCall.function.ret, functionCall.inline);
	}

	private Maybe<String> visit(SemaExprImplicitConversionLValueToRValue conversion)
	{
		if(Types.isZeroSized(conversion.type()))
			return Maybe.none();

		String lvalueSsa = generate(conversion.expr, context, fcontext).unwrap();
		String lvalueTypeString = TypeCodeGenerator.generate(conversion.type(), context.platform());
		return Maybe.some(generateLoad(lvalueSsa, lvalueTypeString));
	}

	private Maybe<String> visit(SemaExprImplicitConversionNonNullablePointerToNullablePointer conversion)
	{
		return generate(conversion.expr, context, fcontext);
	}

	private Maybe<String> visit(SemaExprImplicitConversionNullToNullablePointer conversion)
	{
		return Maybe.some("null");
	}

	private Maybe<String> visit(SemaExprImplicitConversionPointerToNonConstToPointerToConst conversion)
	{
		return generate(conversion.expr, context, fcontext);
	}

	private Maybe<String> visit(SemaExprImplicitConversionPointerToBytePointer conversion)
	{
		String exprTypeString = TypeCodeGenerator.generate(conversion.expr.type(), context.platform());
		String exprSsa = generate(conversion.expr, context, fcontext).unwrap();
		String resultSsa = fcontext.allocateSsa();
		context.writef("\t%s = bitcast %s %s to i8*\n", resultSsa, exprTypeString, exprSsa);
		return Maybe.some(resultSsa);
	}

	private Maybe<String> visit(SemaExprImplicitConversionWiden conversion)
	{
		String valueSsa = generate(conversion.expr, context, fcontext).unwrap();
		return Maybe.some(generateCast(valueSsa, conversion.expr.type(), conversion.type(), SemaExprCast.Kind.WIDEN_CAST));
	}

	private Maybe<String> visit(SemaExprImplicitVoidInReturn implicitVoid)
	{
		return Maybe.none();
	}

	private Maybe<String> visit(SemaExprIndirectFunctionCall functionCall)
	{
		String functionSsa = generate(functionCall.expr, context, fcontext).unwrap();
		return generateFunctionCall(functionSsa, functionCall.args, functionCall.type(), Maybe.none());
	}

	private Maybe<String> visit(SemaExprFieldAccess fieldAccess)
	{
		String objectSsa = generate(fieldAccess.expr, context, fcontext).unwrap();

		boolean isRValue = fieldAccess.expr.kind() == ExprKind.RVALUE;

		if(isRValue)
			fieldAccess.expr = new RValueToLValueConversion(fieldAccess.expr);

		SemaType objectType = fieldAccess.expr.type();
		String objectTypeString = TypeCodeGenerator.generate(objectType, context.platform());
		int fieldIndex = fieldAccess.field.index;
		SemaType fieldType = fieldAccess.type();
		String fieldTypeString = TypeCodeGenerator.generate(fieldType, context.platform());
		String fieldPtrSsa = fcontext.allocateSsa();
		context.writef("\t%s = getelementptr %s, %s* %s, i32 0, i32 %s\n", fieldPtrSsa, objectTypeString, objectTypeString, objectSsa, fieldIndex);

		if(isRValue)
			return Maybe.some(generateLoad(fieldPtrSsa, fieldTypeString));

		return Maybe.some(fieldPtrSsa);
	}

	private Maybe<String> visit(SemaExprLitArray lit)
	{
		// array literals should not generate any temporary variables as they are required to be literals
		// hence we do not pass a builder to this context (null)
		FileCodegenContext tmpContext = new FileCodegenContext(context.project(), context.platform(), null, context.stringPool());
		StringBuilder builder = new StringBuilder();

		builder.append('[');

		if(!lit.values.isEmpty())
		{
			builder.append(TypeCodeGenerator.generate(lit.values.get(0).type(), tmpContext.platform()));
			builder.append(' ');
			builder.append(generate(lit.values.get(0), tmpContext, null).unwrap());

			for(int i = 1; i != lit.values.size(); ++i)
			{
				SemaExpr expr = lit.values.get(i);
				builder.append(", ");
				builder.append(TypeCodeGenerator.generate(expr.type(), tmpContext.platform()));
				builder.append(' ');
				builder.append(generate(expr, tmpContext, null).unwrap());
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

	private Maybe<String> visit(SemaExprLitString lit)
	{
		return Maybe.some(context.stringPool().get(lit.value));
	}

	private Maybe<String> visit(SemaExprNamedFunc namedFunc)
	{
		if(namedFunc.func instanceof SemaDeclExternFunction)
			return Maybe.some("@" + ((SemaDeclExternFunction)namedFunc.func).externName.or(namedFunc.func.name()));

		return Maybe.some("@" + FunctionNameMangler.mangle(namedFunc.func));
	}

	private Maybe<String> visit(SemaExprNamedGlobal namedGlobal)
	{
		return Maybe.some('@' + namedGlobal.global.qualifiedName().toString());
	}

	private Maybe<String> visit(SemaExprNamedLocal namedLocal)
	{
		return Maybe.some('%' + namedLocal.local.name);
	}

	private Maybe<String> visit(SemaExprNamedParam namedParam)
	{
		return Maybe.some('%' + namedParam.param.name);
	}

	private Maybe<String> visit(SemaExprOffsetof offsetof)
	{
		return Maybe.some("" + offsetof.field.offsetof());
	}

	private Maybe<String> visit(SemaExprSizeof sizeof)
	{
		return Maybe.some("" + Types.sizeof(sizeof.type, context.platform()));
	}

	private Maybe<String> visit(SsaExpr ssa)
	{
		return Maybe.some(ssa.name);
	}
}
