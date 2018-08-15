// Copyright 2016-2018 Markus Grech
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
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Fraction;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ExprCodeGenerator implements IVisitor
{
	private static final String ZEROSIZE_VALUE_ADDRESS = "inttoptr (i8 1 to i8*)";
	private static final String UNDEF = "undef";

	private static final SemaExpr INDEX_ZERO_EXPR = new SemaExprLitInt(BigInteger.ZERO, BuiltinType.INT32);
	private static final SemaExpr INDEX_ONE_EXPR = new SemaExprLitInt(BigInteger.ONE, BuiltinType.INT32);

	private FunctionCodegenContext context;

	private ExprCodeGenerator(FunctionCodegenContext context)
	{
		this.context = context;
	}

	public static Maybe<String> generate(SemaExpr expr, FunctionCodegenContext context)
	{
		var visitor = new ExprCodeGenerator(context);
		return (Maybe<String>)expr.accept(visitor);
	}

	private String generateLoad(String where, String type)
	{
		var ssa = context.allocateSsa();
		context.writef("\t%s = load %s, %s* %s\n", ssa, type, type, where);
		return ssa;
	}

	private void generateStore(String where, String what, String type)
	{
		context.writef("\tstore %s %s, %s* %s\n", type, what, type, where);
	}

	private Maybe<String> visit(RValueToLValueConversion conversion)
	{
		var exprSsa = generate(conversion.expr, context).unwrap();
		var exprType = TypeCodeGenerator.generate(conversion.expr.type(), context.platform());
		var resultSsa = context.allocateSsa();
		generateStore(resultSsa, exprSsa, exprType);
		return Maybe.some(resultSsa);
	}

	private Maybe<String> visit(SemaExprAddressof addressof)
	{
		if(Types.isZeroSized(addressof.expr.type()))
			return Maybe.some(ZEROSIZE_VALUE_ADDRESS);

		return generate(addressof.expr, context);
	}

	private Maybe<String> visit(SemaExprAlignof alignof)
	{
		return Maybe.some("" + Types.alignof(alignof.type, context.platform()));
	}

	private String generateGetElementPtr(SemaExpr compound, SemaExpr index)
	{
		var compoundSsa = generate(compound, context).unwrap();

		var baseType = Types.removePointer(compound.type());
		var baseTypeString = TypeCodeGenerator.generate(baseType, context.platform());

		var indexTypeString = TypeCodeGenerator.generate(index.type(), context.platform());
		var indexSsa = generate(index, context).unwrap();

		var resultSsa = context.allocateSsa();
		context.writef("\t%s = getelementptr %s, %s* %s, i64 0, %s %s\n", resultSsa, baseTypeString, baseTypeString, compoundSsa, indexTypeString, indexSsa);
		return resultSsa;
	}

	private Maybe<String> visit(SemaExprArrayAccess arrayAccess)
	{
		if(Types.isZeroSized(arrayAccess.type()))
			return Maybe.none();

		var isRValue = arrayAccess.expr.kind() == ExprKind.RVALUE;

		if(isRValue)
			arrayAccess.expr = new RValueToLValueConversion(arrayAccess.expr);

		var resultSsa = generateGetElementPtr(arrayAccess.expr, arrayAccess.index);

		if(isRValue)
		{
			var resultTypeString = TypeCodeGenerator.generate(arrayAccess.type(), context.platform());
			return Maybe.some(generateLoad(resultSsa, resultTypeString));
		}

		return Maybe.some(resultSsa);
	}

	private Maybe<String> visit(SemaExprArrayGetLength arrayGetLength)
	{
		return Maybe.some("" + Types.arrayLength(arrayGetLength.expr.type()));
	}

	private Maybe<String> visit(SemaExprArrayGetPointer arrayGetPointer)
	{
		return Maybe.some(generateGetElementPtr(arrayGetPointer.expr, INDEX_ZERO_EXPR));
	}

	private String generateSliceConstruction(SemaType elementType, String pointerSsa, long length)
	{
		var pointerType = Types.addNullablePointer(elementType);
		var pointerTypeString = TypeCodeGenerator.generate(pointerType, context.platform());
		var lengthTypeString = TypeCodeGenerator.generate(SemaTypeBuiltin.INT, context.platform());
		var structTypeString = String.format("{%s, %s}", pointerTypeString, lengthTypeString);

		var intermediateSsa = generateInsertValue(structTypeString, UNDEF, pointerTypeString, pointerSsa, 0);
		return generateInsertValue(structTypeString, intermediateSsa, lengthTypeString, "" + length, 1);
	}

	private Maybe<String> visit(SemaExprArrayGetSlice arrayGetSlice)
	{
		var elementType = Types.removeSlice(arrayGetSlice.type());
		var pointerSsa = generateGetElementPtr(arrayGetSlice.expr, INDEX_ZERO_EXPR);
		var length = Types.arrayLength(Types.removePointer(arrayGetSlice.expr.type()));
		return Maybe.some(generateSliceConstruction(elementType, pointerSsa, length));
	}

	private Maybe<String> visit(SemaExprAssign assign)
	{
		if(Types.isZeroSized(assign.type()))
			return Maybe.none();

		var typeString = TypeCodeGenerator.generate(assign.right.type(), context.platform());
		var right = generate(assign.right, context).unwrap();
		var left = generate(assign.left, context).unwrap();
		generateStore(left, right, typeString);
		return Maybe.some(left);
	}

	private Maybe<String> visit(SemaExprBuiltinCall builtinCall)
	{
		return builtinCall.func.generateCall(builtinCall, context);
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
		var resultSsa = context.allocateSsa();
		var sourceTypeString = TypeCodeGenerator.generate(sourceType, context.platform());
		var targetTypeString = TypeCodeGenerator.generate(targetType, context.platform());

		var instr = instrForCast(targetType, kind);
		context.writef("\t%s = %s %s %s to %s\n", resultSsa, instr, sourceTypeString, valueSsa, targetTypeString);
		return resultSsa;
	}

	private Maybe<String> visit(SemaExprCast cast)
	{
		var valueSsa = generate(cast.expr, context).unwrap();
		String resultSsa;

		var sourceType = cast.expr.type();
		var targetType = cast.type;

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
		return generate(const_.expr, context);
	}

	private Maybe<String> visit(SemaExprDeref deref)
	{
		if(Types.isZeroSized(deref.type()))
			return Maybe.none();

		return generate(deref.expr, context);
	}

	private Maybe<String> generateFunctionCall(String functionSsa, List<SemaExpr> args, SemaType ret, Maybe<Boolean> inline)
	{
		var argsSsa = new ArrayList<String>();

		for(var arg : args)
			argsSsa.add(generate(arg, context).unwrap());

		context.write('\t');

		Maybe<String> retSsa = Maybe.none();

		if(!Types.isZeroSized(ret))
		{
			retSsa = Maybe.some(context.allocateSsa());
			context.writef("%s = ", retSsa.unwrap());
		}

		var retTypeString = TypeCodeGenerator.generate(ret, context.platform());

		context.writef("call %s %s(", retTypeString, functionSsa);

		if(!args.isEmpty())
		{
			if(!Types.isZeroSized(args.get(0).type()))
			{
				context.write(TypeCodeGenerator.generate(args.get(0).type(), context.platform()));
				context.write(' ');
				context.write(argsSsa.get(0));
			}

			for(var i = 1; i != argsSsa.size(); ++i)
			{
				if(!Types.isZeroSized(args.get(i).type()))
				{
					var argTypeString = TypeCodeGenerator.generate(args.get(i).type(), context.platform());
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

		var functionSsa = '@' + name;
		return generateFunctionCall(functionSsa, functionCall.args, functionCall.function.ret, functionCall.inline);
	}

	private Maybe<String> visit(SemaExprImplicitConversionArrayPointerToPointer conversion)
	{
		return Maybe.some(generateGetElementPtr(conversion.expr, INDEX_ZERO_EXPR));
	}

	private String generateInsertValue(String compoundTypeString, String compoundSsa, String elementTypeString, String elementSsa, int index)
	{
		var ssa = context.allocateSsa();
		context.writef("\t%s = insertvalue %s %s, %s %s, %s\n", ssa, compoundTypeString, compoundSsa, elementTypeString, elementSsa, index);
		return ssa;
	}

	private Maybe<String> visit(SemaExprImplicitConversionArrayPointerToSlice conversion)
	{
		var pointerSsa = generateGetElementPtr(conversion.expr, INDEX_ZERO_EXPR);
		var elementType = Types.removeSlice(conversion.type());
		var length = Types.arrayLength(Types.removePointer(conversion.expr.type()));
		return Maybe.some(generateSliceConstruction(elementType, pointerSsa, length));
	}

	private Maybe<String> visit(SemaExprImplicitConversionLValueToRValue conversion)
	{
		if(Types.isZeroSized(conversion.type()))
			return Maybe.none();

		var lvalueSsa = generate(conversion.expr, context).unwrap();
		var lvalueTypeString = TypeCodeGenerator.generate(conversion.type(), context.platform());
		return Maybe.some(generateLoad(lvalueSsa, lvalueTypeString));
	}

	private Maybe<String> visit(SemaExprImplicitConversionNonNullablePointerToNullablePointer conversion)
	{
		return generate(conversion.expr, context);
	}

	private Maybe<String> visit(SemaExprImplicitConversionNullToNullablePointer conversion)
	{
		return Maybe.some("null");
	}

	private Maybe<String> visit(SemaExprImplicitConversionPointerToNonConstToPointerToConst conversion)
	{
		return generate(conversion.expr, context);
	}

	private Maybe<String> visit(SemaExprImplicitConversionPointerToBytePointer conversion)
	{
		var exprTypeString = TypeCodeGenerator.generate(conversion.expr.type(), context.platform());
		var exprSsa = generate(conversion.expr, context).unwrap();
		var resultSsa = context.allocateSsa();
		context.writef("\t%s = bitcast %s %s to i8*\n", resultSsa, exprTypeString, exprSsa);
		return Maybe.some(resultSsa);
	}

	private Maybe<String> visit(SemaExprImplicitConversionSliceToSliceOfConst conversion)
	{
		return generate(conversion.expr, context);
	}

	private Maybe<String> visit(SemaExprImplicitConversionWiden conversion)
	{
		var valueSsa = generate(conversion.expr, context).unwrap();
		return Maybe.some(generateCast(valueSsa, conversion.expr.type(), conversion.type(), SemaExprCast.Kind.WIDEN_CAST));
	}

	private Maybe<String> visit(SemaExprImplicitVoidInReturn implicitVoid)
	{
		return Maybe.none();
	}

	private Maybe<String> visit(SemaExprIndirectFunctionCall functionCall)
	{
		var functionSsa = generate(functionCall.expr, context).unwrap();
		return generateFunctionCall(functionSsa, functionCall.args, functionCall.type(), Maybe.none());
	}

	private Maybe<String> visit(SemaExprFieldAccess fieldAccess)
	{
		var isRValue = fieldAccess.expr.kind() == ExprKind.RVALUE;

		if(isRValue)
			fieldAccess.expr = new RValueToLValueConversion(fieldAccess.expr);

		var indexExpr = new SemaExprLitInt(BigInteger.valueOf(fieldAccess.field.index), BuiltinType.INT32);
		var resultSsa = generateGetElementPtr(fieldAccess.expr, indexExpr);

		if(isRValue)
		{
			var fieldTypeString = TypeCodeGenerator.generate(indexExpr.type(), context.platform());
			return Maybe.some(generateLoad(resultSsa, fieldTypeString));
		}

		return Maybe.some(resultSsa);
	}

	private Maybe<String> visit(SemaExprLitArray lit)
	{
		var builder = new StringBuilder();
		builder.append('[');

		if(!lit.values.isEmpty())
		{
			builder.append(TypeCodeGenerator.generate(lit.values.get(0).type(), context.platform()));
			builder.append(' ');
			builder.append(generate(lit.values.get(0), context).unwrap());

			for(var i = 1; i != lit.values.size(); ++i)
			{
				SemaExpr expr = lit.values.get(i);
				builder.append(", ");
				builder.append(TypeCodeGenerator.generate(expr.type(), context.platform()));
				builder.append(' ');
				builder.append(generate(expr, context).unwrap());
			}
		}

		builder.append(']');

		return Maybe.some(builder.toString());
	}

	private Maybe<String> visit(SemaExprLitBool lit)
	{
		return Maybe.some("" + lit.value);
	}

	private String toFloatHexString(Fraction f)
	{
		// llvm requires float literals to be as wide as double literals but representable in a float
		// hence we take the float value and widen it to double
		var d = f.toFloat();
		var l = Double.doubleToLongBits(d);
		return String.format("0x%x", l);
	}

	private String toDoubleHexString(Fraction f)
	{
		var d = f.toDouble();
		var l = Double.doubleToRawLongBits(d);
		return String.format("0x%x", l);
	}

	private Maybe<String> visit(SemaExprLitFloat lit)
	{
		var s = lit.type == BuiltinType.FLOAT32
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

	private String generateExtractValue(SemaExpr compound, int index)
	{
		var compoundSsa = generate(compound, context).unwrap();
		var compoundTypeString = TypeCodeGenerator.generate(compound.type(), context.platform());

		var fieldSsa = context.allocateSsa();
		context.writef("\t%s = extractvalue %s %s, %s\n", fieldSsa, compoundTypeString, compoundSsa, index);
		return fieldSsa;
	}

	private Maybe<String> visit(SemaExprSliceGetLength sliceGetLength)
	{
		if(sliceGetLength.expr.kind() == ExprKind.RVALUE)
			return Maybe.some(generateExtractValue(sliceGetLength.expr, 1));

		return Maybe.some(generateGetElementPtr(sliceGetLength.expr, INDEX_ONE_EXPR));
	}

	private Maybe<String> visit(SemaExprSliceGetPointer sliceGetPointer)
	{
		if(sliceGetPointer.expr.kind() == ExprKind.RVALUE)
			return Maybe.some(generateExtractValue(sliceGetPointer.expr, 0));

		return Maybe.some(generateGetElementPtr(sliceGetPointer.expr, INDEX_ZERO_EXPR));
	}
}
