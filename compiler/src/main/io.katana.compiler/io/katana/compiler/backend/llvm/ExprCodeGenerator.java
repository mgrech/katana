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
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.math.BigInteger;
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
		return Maybe.wrap((IrValue)expr.accept(visitor));
	}

	private IrValue lower(SemaExpr expr)
	{
		return (IrValue)expr.accept(this);
	}

	private IrType lower(SemaType type)
	{
		return TypeCodeGenerator.generate(type, context.platform());
	}

	private IrValueSsa generateLoad(IrValue pointer, IrType type)
	{
		var result = context.allocateSsa();
		context.writef("\t%s = load %s, %s* %s\n", result, type, type, pointer);
		return result;
	}

	private void generateStore(IrValue pointer, IrValue value, IrType type)
	{
		context.writef("\tstore %s %s, %s* %s\n", type, value, type, pointer);
	}

	private IrValueSsa generateAlloca(IrType type, long alignment)
	{
		var result = context.allocateSsa();
		context.writef("\t%s = alloca %s, align %s\n", result, type, alignment);
		return result;
	}

	private IrValue visit(RValueToLValueConversion conversion)
	{
		var value = lower(conversion.expr);
		var type = conversion.expr.type();
		var alignment = Types.alignof(type, context.platform());
		var typeIr = lower(type);
		var pointer = generateAlloca(typeIr, alignment);
		generateStore(pointer, value, typeIr);
		return pointer;
	}

	private IrValue visit(SemaExprAddressof addressof)
	{
		if(Types.isZeroSized(addressof.expr.type()))
			return IrValues.ADDRESS_ONE;

		return lower(addressof.expr);
	}

	private IrValue visit(SemaExprAlignof alignof)
	{
		return IrValues.ofConstant(Types.alignof(alignof.type, context.platform()));
	}

	private IrValueSsa generateGetElementPtr(SemaExpr compoundExpr, SemaExpr indexExpr)
	{
		var compound = lower(compoundExpr);
		var baseType = lower(Types.removePointer(compoundExpr.type()));
		var indexType = lower(indexExpr.type());
		var index = lower(indexExpr);

		var result = context.allocateSsa();
		context.writef("\t%s = getelementptr %s, %s* %s, i64 0, %s %s\n", result, baseType, baseType, compound, indexType, index);
		return result;
	}

	private IrValue visit(SemaExprArrayAccess arrayAccess)
	{
		if(Types.isZeroSized(arrayAccess.type()))
			return null;

		var isRValue = arrayAccess.expr.kind() == ExprKind.RVALUE;

		if(isRValue)
			arrayAccess.expr = new RValueToLValueConversion(arrayAccess.expr);

		var result = generateGetElementPtr(arrayAccess.expr, arrayAccess.index);

		if(isRValue)
		{
			var resultType = lower(arrayAccess.type());
			return generateLoad(result, resultType);
		}

		return result;
	}

	private IrValue visit(SemaExprArrayGetLength arrayGetLength)
	{
		return IrValues.ofConstant(Types.arrayLength(arrayGetLength.expr.type()));
	}

	private IrValue visit(SemaExprArrayGetPointer arrayGetPointer)
	{
		return generateGetElementPtr(arrayGetPointer.expr, INDEX_ZERO_EXPR);
	}

	private IrValueSsa generateSliceConstruction(SemaType elementType, IrValue pointer, long length)
	{
		var sliceType = (IrTypeStructLiteral)lower(Types.addSlice(elementType));
		var pointerType = sliceType.fields.get(0);
		var lengthType = sliceType.fields.get(1);

		var intermediate = generateInsertValue(sliceType, IrValues.UNDEF, pointerType, pointer, 0);
		return generateInsertValue(sliceType, intermediate, lengthType, IrValues.ofConstant(length), 1);
	}

	private IrValue visit(SemaExprArrayGetSlice arrayGetSlice)
	{
		var elementType = Types.removeSlice(arrayGetSlice.type());
		var pointer = generateGetElementPtr(arrayGetSlice.expr, INDEX_ZERO_EXPR);
		var length = Types.arrayLength(Types.removePointer(arrayGetSlice.expr.type()));
		return generateSliceConstruction(elementType, pointer, length);
	}

	private IrValue visit(SemaExprAssign assign)
	{
		if(Types.isZeroSized(assign.type()))
			return null;

		var type = lower(assign.right.type());
		var right = lower(assign.right);
		var left = lower(assign.left);
		generateStore(left, right, type);
		return left;
	}

	private IrValue visit(SemaExprBuiltinCall builtinCall)
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

	private IrValueSsa generateCast(IrValue value, SemaType sourceType, SemaType targetType, SemaExprCast.Kind kind)
	{
		var result = context.allocateSsa();
		var sourceTypeIr = lower(sourceType);
		var targetTypeIr = lower(targetType);

		var instr = instrForCast(targetType, kind);
		context.writef("\t%s = %s %s %s to %s\n", result, instr, sourceTypeIr, value, targetTypeIr);
		return result;
	}

	private IrValue visit(SemaExprCast cast)
	{
		var value = lower(cast.expr);

		var sourceType = cast.expr.type();
		var targetType = cast.type;

		switch(cast.kind)
		{
		case SIGN_CAST:
			return value;

		case WIDEN_CAST:
			if(Types.equalSizes(sourceType, targetType, context.platform()))
				return value;

			return generateCast(value, sourceType, targetType, SemaExprCast.Kind.WIDEN_CAST);

		case NARROW_CAST:
			if(Types.equalSizes(sourceType, targetType, context.platform()))
				return value;

			return generateCast(value, sourceType, targetType, SemaExprCast.Kind.NARROW_CAST);

		case POINTER_CAST:
			if(Types.equal(Types.removeConst(sourceType), Types.removeConst(targetType)))
				return value;

			return generateCast(value, sourceType, targetType, SemaExprCast.Kind.POINTER_CAST);

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private IrValue visit(SemaExprConst const_)
	{
		return lower(const_.expr);
	}

	private IrValue visit(SemaExprDeref deref)
	{
		if(Types.isZeroSized(deref.type()))
			return null;

		return lower(deref.expr);
	}

	private IrValue generateFunctionCall(IrValue function, List<SemaExpr> args, SemaType returnType, Maybe<Boolean> inline)
	{
		var argsIr = args.stream()
		                 .map(this::lower)
		                 .collect(Collectors.toList());

		context.write('\t');

		Maybe<IrValue> returnValue = Maybe.none();

		if(!Types.isZeroSized(returnType))
		{
			returnValue = Maybe.some(context.allocateSsa());
			context.writef("%s = ", returnValue.unwrap());
		}

		var returnTypeIr = lower(returnType);
		context.writef("call %s %s(", returnTypeIr, function);

		if(!args.isEmpty())
		{
			if(!Types.isZeroSized(args.get(0).type()))
			{
				context.write(lower(args.get(0).type()));
				context.write(' ');
				context.write(argsIr.get(0));
			}

			for(var i = 1; i != argsIr.size(); ++i)
			{
				if(!Types.isZeroSized(args.get(i).type()))
				{
					var argType = lower(args.get(i).type());
					context.writef(", %s %s", argType, argsIr.get(i));
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

		return returnValue.unwrap();
	}

	private IrValue visit(SemaExprDirectFunctionCall call)
	{
		String name;

		if(call.function instanceof SemaDeclExternFunction)
		{
			var function = (SemaDeclExternFunction)call.function;
			name = function.externName.or(function.name());
		}
		else
			name = FunctionNameMangler.mangle(call.function);

		var function = IrValues.ofSymbol(name);
		return generateFunctionCall(function, call.args, call.function.ret, call.inline);
	}

	private IrValue visit(SemaExprImplicitConversionArrayPointerToPointer conversion)
	{
		return generateGetElementPtr(conversion.expr, INDEX_ZERO_EXPR);
	}

	private IrValueSsa generateInsertValue(IrType compoundType, IrValue compound, IrType elementType, IrValue element, int index)
	{
		var result = context.allocateSsa();
		context.writef("\t%s = insertvalue %s %s, %s %s, %s\n", result, compoundType, compound, elementType, element, index);
		return result;
	}

	private IrValue visit(SemaExprImplicitConversionArrayPointerToSlice conversion)
	{
		var pointer = generateGetElementPtr(conversion.expr, INDEX_ZERO_EXPR);
		var length = Types.arrayLength(Types.removePointer(conversion.expr.type()));
		var elementType = Types.removeSlice(conversion.type());
		return generateSliceConstruction(elementType, pointer, length);
	}

	private IrValue visit(SemaExprImplicitConversionLValueToRValue conversion)
	{
		if(Types.isZeroSized(conversion.type()))
			return null;

		var value = lower(conversion.expr);
		var type = lower(conversion.type());
		return generateLoad(value, type);
	}

	private IrValue visit(SemaExprImplicitConversionNonNullablePointerToNullablePointer conversion)
	{
		return lower(conversion.expr);
	}

	private IrValue visit(SemaExprImplicitConversionNullToNullablePointer conversion)
	{
		return IrValues.NULL;
	}

	private IrValue visit(SemaExprImplicitConversionPointerToNonConstToPointerToConst conversion)
	{
		return lower(conversion.expr);
	}

	private IrValue visit(SemaExprImplicitConversionPointerToBytePointer conversion)
	{
		var pointerType = lower(conversion.expr.type());
		var pointer = lower(conversion.expr);
		var result = context.allocateSsa();
		context.writef("\t%s = bitcast %s %s to i8*\n", result, pointerType, pointer);
		return result;
	}

	private IrValue visit(SemaExprImplicitConversionSliceToSliceOfConst conversion)
	{
		return lower(conversion.expr);
	}

	private IrValue visit(SemaExprImplicitConversionWiden conversion)
	{
		var value = lower(conversion.expr);
		var sourceType = conversion.expr.type();
		var targetType = conversion.type();
		return generateCast(value, sourceType, targetType, SemaExprCast.Kind.WIDEN_CAST);
	}

	private IrValue visit(SemaExprImplicitVoidInReturn implicitVoid)
	{
		return null;
	}

	private IrValue visit(SemaExprIndirectFunctionCall functionCall)
	{
		var function = lower(functionCall.expr);
		return generateFunctionCall(function, functionCall.args, functionCall.type(), Maybe.none());
	}

	private IrValue visit(SemaExprFieldAccess fieldAccess)
	{
		var isRValue = fieldAccess.expr.kind() == ExprKind.RVALUE;

		if(isRValue)
			fieldAccess.expr = new RValueToLValueConversion(fieldAccess.expr);

		var index = new SemaExprLitInt(BigInteger.valueOf(fieldAccess.field.index), BuiltinType.INT32);
		var result = generateGetElementPtr(fieldAccess.expr, index);

		if(isRValue)
		{
			var fieldType = lower(index.type());
			return generateLoad(result, fieldType);
		}

		return result;
	}

	private IrValue visit(SemaExprLitArray lit)
	{
		var elementType = lower(lit.type);
		var values = lit.values.stream()
		                       .map(this::lower)
		                       .collect(Collectors.toList());

		return IrValues.ofConstantArray(elementType, values);
	}

	private IrValue visit(SemaExprLitBool lit)
	{
		return IrValues.ofConstant(lit.value);
	}

	private IrValue visit(SemaExprLitFloat lit)
	{
		return lit.type == BuiltinType.FLOAT32
		       ? IrValues.ofConstant(lit.value.toFloat())
		       : IrValues.ofConstant(lit.value.toDouble());
	}

	private IrValue visit(SemaExprLitInt lit)
	{
		return IrValues.ofConstant(lit.value.longValueExact());
	}

	private IrValue visit(SemaExprLitNull lit)
	{
		return IrValues.NULL;
	}

	private IrValue visit(SemaExprLitString lit)
	{
		return context.stringPool().get(lit.value);
	}

	private IrValue visit(SemaExprNamedFunc namedFunc)
	{
		if(namedFunc.func instanceof SemaDeclExternFunction)
		{
			var name = ((SemaDeclExternFunction)namedFunc.func).externName.or(namedFunc.func.name());
			return IrValues.ofSymbol(name);
		}

		var name = FunctionNameMangler.mangle(namedFunc.func);
		return IrValues.ofSymbol(name);
	}

	private IrValue visit(SemaExprNamedGlobal namedGlobal)
	{
		var name = namedGlobal.global.qualifiedName().toString();
		return IrValues.ofSymbol(name);
	}

	private IrValue visit(SemaExprNamedLocal namedLocal)
	{
		return IrValues.ofSsa(namedLocal.local.name);
	}

	private IrValue visit(SemaExprNamedParam namedParam)
	{
		return IrValues.ofSsa(namedParam.param.name);
	}

	private IrValue visit(SemaExprOffsetof offsetof)
	{
		var offset = offsetof.field.offsetof();
		return IrValues.ofConstant(offset);
	}

	private IrValue visit(SemaExprSizeof sizeof)
	{
		var size = Types.sizeof(sizeof.type, context.platform());
		return IrValues.ofConstant(size);
	}

	private IrValueSsa generateExtractValue(SemaExpr compound, int index)
	{
		var compoundIr = lower(compound);
		var compoundType = lower(compound.type());

		var result = context.allocateSsa();
		context.writef("\t%s = extractvalue %s %s, %s\n", result, compoundType, compoundIr, index);
		return result;
	}

	private IrValue visit(SemaExprSliceGetLength sliceGetLength)
	{
		if(sliceGetLength.expr.kind() == ExprKind.RVALUE)
			return generateExtractValue(sliceGetLength.expr, 1);

		return generateGetElementPtr(sliceGetLength.expr, INDEX_ONE_EXPR);
	}

	private IrValue visit(SemaExprSliceGetPointer sliceGetPointer)
	{
		if(sliceGetPointer.expr.kind() == ExprKind.RVALUE)
			return generateExtractValue(sliceGetPointer.expr, 0);

		return generateGetElementPtr(sliceGetPointer.expr, INDEX_ZERO_EXPR);
	}
}
