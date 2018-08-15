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
import io.katana.compiler.backend.llvm.ir.*;
import io.katana.compiler.sema.decl.SemaDeclExternFunction;
import io.katana.compiler.sema.expr.*;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ExprCodeGenerator implements IVisitor
{
	private static final SemaExpr INDEX_ZERO_EXPR = new SemaExprLitInt(BigInteger.ZERO, BuiltinType.INT32);
	private static final SemaExpr INDEX_ONE_EXPR = new SemaExprLitInt(BigInteger.ONE, BuiltinType.INT32);

	private FunctionCodegenContext context;

	private ExprCodeGenerator(FunctionCodegenContext context)
	{
		this.context = context;
	}

	public static Maybe<IrValue> generate(SemaExpr expr, FunctionCodegenContext context)
	{
		var visitor = new ExprCodeGenerator(context);
		return (Maybe<IrValue>)expr.accept(visitor);
	}

	private IrValueSsa generateLoad(IrValue where, String type)
	{
		var ssa = context.allocateSsa();
		context.writef("\t%s = load %s, %s* %s\n", ssa, type, type, where);
		return ssa;
	}

	private void generateStore(IrValue where, IrValue what, String type)
	{
		context.writef("\tstore %s %s, %s* %s\n", type, what, type, where);
	}

	private Maybe<IrValue> visit(RValueToLValueConversion conversion)
	{
		var exprSsa = generate(conversion.expr, context).unwrap();
		var exprType = TypeCodeGenerator.generate(conversion.expr.type(), context.platform());
		var resultSsa = context.allocateSsa();
		generateStore(resultSsa, exprSsa, exprType);
		return Maybe.some(resultSsa);
	}

	private Maybe<IrValue> visit(SemaExprAddressof addressof)
	{
		if(Types.isZeroSized(addressof.expr.type()))
			return Maybe.some(IrValues.ADDRESS_ONE);

		return generate(addressof.expr, context);
	}

	private Maybe<IrValue> visit(SemaExprAlignof alignof)
	{
		return Maybe.some(IrValues.ofConstant(Types.alignof(alignof.type, context.platform())));
	}

	private IrValueSsa generateGetElementPtr(SemaExpr compound, SemaExpr index)
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

	private Maybe<IrValue> visit(SemaExprArrayAccess arrayAccess)
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

	private Maybe<IrValue> visit(SemaExprArrayGetLength arrayGetLength)
	{
		return Maybe.some(IrValues.ofConstant(Types.arrayLength(arrayGetLength.expr.type())));
	}

	private Maybe<IrValue> visit(SemaExprArrayGetPointer arrayGetPointer)
	{
		return Maybe.some(generateGetElementPtr(arrayGetPointer.expr, INDEX_ZERO_EXPR));
	}

	private IrValueSsa generateSliceConstruction(SemaType elementType, IrValue pointerSsa, long length)
	{
		var pointerType = Types.addNullablePointer(elementType);
		var pointerTypeString = TypeCodeGenerator.generate(pointerType, context.platform());
		var lengthTypeString = TypeCodeGenerator.generate(SemaTypeBuiltin.INT, context.platform());
		var structTypeString = String.format("{%s, %s}", pointerTypeString, lengthTypeString);

		var intermediateSsa = generateInsertValue(structTypeString, IrValues.UNDEF, pointerTypeString, pointerSsa, 0);
		return generateInsertValue(structTypeString, intermediateSsa, lengthTypeString, IrValues.ofConstant(length), 1);
	}

	private Maybe<IrValue> visit(SemaExprArrayGetSlice arrayGetSlice)
	{
		var elementType = Types.removeSlice(arrayGetSlice.type());
		var pointerSsa = generateGetElementPtr(arrayGetSlice.expr, INDEX_ZERO_EXPR);
		var length = Types.arrayLength(Types.removePointer(arrayGetSlice.expr.type()));
		return Maybe.some(generateSliceConstruction(elementType, pointerSsa, length));
	}

	private Maybe<IrValue> visit(SemaExprAssign assign)
	{
		if(Types.isZeroSized(assign.type()))
			return Maybe.none();

		var typeString = TypeCodeGenerator.generate(assign.right.type(), context.platform());
		var right = generate(assign.right, context).unwrap();
		var left = generate(assign.left, context).unwrap();
		generateStore(left, right, typeString);
		return Maybe.some(left);
	}

	private Maybe<IrValue> visit(SemaExprBuiltinCall builtinCall)
	{
		return builtinCall.func.generateCall(builtinCall, context).map(v -> v);
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

	private IrValueSsa generateCast(IrValue valueSsa, SemaType sourceType, SemaType targetType, SemaExprCast.Kind kind)
	{
		var resultSsa = context.allocateSsa();
		var sourceTypeString = TypeCodeGenerator.generate(sourceType, context.platform());
		var targetTypeString = TypeCodeGenerator.generate(targetType, context.platform());

		var instr = instrForCast(targetType, kind);
		context.writef("\t%s = %s %s %s to %s\n", resultSsa, instr, sourceTypeString, valueSsa, targetTypeString);
		return resultSsa;
	}

	private Maybe<IrValue> visit(SemaExprCast cast)
	{
		var valueSsa = generate(cast.expr, context).unwrap();
		IrValue resultSsa;

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

	private Maybe<IrValue> visit(SemaExprConst const_)
	{
		return generate(const_.expr, context);
	}

	private Maybe<IrValue> visit(SemaExprDeref deref)
	{
		if(Types.isZeroSized(deref.type()))
			return Maybe.none();

		return generate(deref.expr, context);
	}

	private Maybe<IrValue> generateFunctionCall(IrValue functionSsa, List<SemaExpr> args, SemaType ret, Maybe<Boolean> inline)
	{
		var argsSsa = new ArrayList<IrValue>();

		for(var arg : args)
			argsSsa.add(generate(arg, context).unwrap());

		context.write('\t');

		Maybe<IrValue> retSsa = Maybe.none();

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

	private Maybe<IrValue> visit(SemaExprDirectFunctionCall functionCall)
	{
		String name;

		if(functionCall.function instanceof SemaDeclExternFunction)
		{
			SemaDeclExternFunction extfn = (SemaDeclExternFunction)functionCall.function;
			name = extfn.externName.or(extfn.name());
		}
		else
			name = FunctionNameMangler.mangle(functionCall.function);

		var functionSsa = new IrValueSymbol(name);
		return generateFunctionCall(functionSsa, functionCall.args, functionCall.function.ret, functionCall.inline);
	}

	private Maybe<IrValue> visit(SemaExprImplicitConversionArrayPointerToPointer conversion)
	{
		return Maybe.some(generateGetElementPtr(conversion.expr, INDEX_ZERO_EXPR));
	}

	private IrValueSsa generateInsertValue(String compoundTypeString, IrValue compoundSsa, String elementTypeString, IrValue elementSsa, int index)
	{
		var ssa = context.allocateSsa();
		context.writef("\t%s = insertvalue %s %s, %s %s, %s\n", ssa, compoundTypeString, compoundSsa, elementTypeString, elementSsa, index);
		return ssa;
	}

	private Maybe<IrValue> visit(SemaExprImplicitConversionArrayPointerToSlice conversion)
	{
		var pointerSsa = generateGetElementPtr(conversion.expr, INDEX_ZERO_EXPR);
		var elementType = Types.removeSlice(conversion.type());
		var length = Types.arrayLength(Types.removePointer(conversion.expr.type()));
		return Maybe.some(generateSliceConstruction(elementType, pointerSsa, length));
	}

	private Maybe<IrValue> visit(SemaExprImplicitConversionLValueToRValue conversion)
	{
		if(Types.isZeroSized(conversion.type()))
			return Maybe.none();

		var lvalueSsa = generate(conversion.expr, context).unwrap();
		var lvalueTypeString = TypeCodeGenerator.generate(conversion.type(), context.platform());
		return Maybe.some(generateLoad(lvalueSsa, lvalueTypeString));
	}

	private Maybe<IrValue> visit(SemaExprImplicitConversionNonNullablePointerToNullablePointer conversion)
	{
		return generate(conversion.expr, context);
	}

	private Maybe<IrValue> visit(SemaExprImplicitConversionNullToNullablePointer conversion)
	{
		return Maybe.some(IrValues.NULL);
	}

	private Maybe<IrValue> visit(SemaExprImplicitConversionPointerToNonConstToPointerToConst conversion)
	{
		return generate(conversion.expr, context);
	}

	private Maybe<IrValue> visit(SemaExprImplicitConversionPointerToBytePointer conversion)
	{
		var exprTypeString = TypeCodeGenerator.generate(conversion.expr.type(), context.platform());
		var exprSsa = generate(conversion.expr, context).unwrap();
		var resultSsa = context.allocateSsa();
		context.writef("\t%s = bitcast %s %s to i8*\n", resultSsa, exprTypeString, exprSsa);
		return Maybe.some(resultSsa);
	}

	private Maybe<IrValue> visit(SemaExprImplicitConversionSliceToSliceOfConst conversion)
	{
		return generate(conversion.expr, context);
	}

	private Maybe<IrValue> visit(SemaExprImplicitConversionWiden conversion)
	{
		var valueSsa = generate(conversion.expr, context).unwrap();
		return Maybe.some(generateCast(valueSsa, conversion.expr.type(), conversion.type(), SemaExprCast.Kind.WIDEN_CAST));
	}

	private Maybe<IrValue> visit(SemaExprImplicitVoidInReturn implicitVoid)
	{
		return Maybe.none();
	}

	private Maybe<IrValue> visit(SemaExprIndirectFunctionCall functionCall)
	{
		var functionSsa = generate(functionCall.expr, context).unwrap();
		return generateFunctionCall(functionSsa, functionCall.args, functionCall.type(), Maybe.none());
	}

	private Maybe<IrValue> visit(SemaExprFieldAccess fieldAccess)
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

	private Maybe<IrValue> visit(SemaExprLitArray lit)
	{
		var elementTypeString = TypeCodeGenerator.generate(lit.type, context.platform());

		var values = lit.values.stream()
		                       .map(expr -> generate(expr, context).unwrap())
		                       .collect(Collectors.toList());

		return Maybe.some(IrValues.ofConstantArray(elementTypeString, values));
	}

	private Maybe<IrValue> visit(SemaExprLitBool lit)
	{
		return Maybe.some(IrValues.ofConstant(lit.value));
	}

	private Maybe<IrValue> visit(SemaExprLitFloat lit)
	{
		var value = lit.type == BuiltinType.FLOAT32
		            ? IrValues.ofConstant(lit.value.toFloat())
		            : IrValues.ofConstant(lit.value.toDouble());

		return Maybe.some(value);
	}

	private Maybe<IrValue> visit(SemaExprLitInt lit)
	{
		return Maybe.some(IrValues.ofConstant(lit.value.longValueExact()));
	}

	private Maybe<IrValue> visit(SemaExprLitNull lit)
	{
		return Maybe.some(IrValues.NULL);
	}

	private Maybe<IrValue> visit(SemaExprLitString lit)
	{
		return Maybe.some(context.stringPool().get(lit.value));
	}

	private Maybe<IrValue> visit(SemaExprNamedFunc namedFunc)
	{
		if(namedFunc.func instanceof SemaDeclExternFunction)
		{
			var name = ((SemaDeclExternFunction)namedFunc.func).externName.or(namedFunc.func.name());
			return Maybe.some(IrValues.ofSymbol(name));
		}

		var name = FunctionNameMangler.mangle(namedFunc.func);
		return Maybe.some(IrValues.ofSymbol(name));
	}

	private Maybe<IrValue> visit(SemaExprNamedGlobal namedGlobal)
	{
		return Maybe.some(IrValues.ofSymbol(namedGlobal.global.qualifiedName().toString()));
	}

	private Maybe<IrValue> visit(SemaExprNamedLocal namedLocal)
	{
		return Maybe.some(IrValues.ofSsa(namedLocal.local.name));
	}

	private Maybe<IrValue> visit(SemaExprNamedParam namedParam)
	{
		return Maybe.some(IrValues.ofSsa(namedParam.param.name));
	}

	private Maybe<IrValue> visit(SemaExprOffsetof offsetof)
	{
		return Maybe.some(IrValues.ofConstant(offsetof.field.offsetof()));
	}

	private Maybe<IrValue> visit(SemaExprSizeof sizeof)
	{
		return Maybe.some(IrValues.ofConstant(Types.sizeof(sizeof.type, context.platform())));
	}

	private IrValueSsa generateExtractValue(SemaExpr compound, int index)
	{
		var compoundSsa = generate(compound, context).unwrap();
		var compoundTypeString = TypeCodeGenerator.generate(compound.type(), context.platform());

		var fieldSsa = context.allocateSsa();
		context.writef("\t%s = extractvalue %s %s, %s\n", fieldSsa, compoundTypeString, compoundSsa, index);
		return fieldSsa;
	}

	private Maybe<IrValue> visit(SemaExprSliceGetLength sliceGetLength)
	{
		if(sliceGetLength.expr.kind() == ExprKind.RVALUE)
			return Maybe.some(generateExtractValue(sliceGetLength.expr, 1));

		return Maybe.some(generateGetElementPtr(sliceGetLength.expr, INDEX_ONE_EXPR));
	}

	private Maybe<IrValue> visit(SemaExprSliceGetPointer sliceGetPointer)
	{
		if(sliceGetPointer.expr.kind() == ExprKind.RVALUE)
			return Maybe.some(generateExtractValue(sliceGetPointer.expr, 0));

		return Maybe.some(generateGetElementPtr(sliceGetPointer.expr, INDEX_ZERO_EXPR));
	}
}
