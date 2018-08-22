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

package io.katana.compiler.backend.llvm.lowering;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.Inlining;
import io.katana.compiler.analysis.Types;
import io.katana.compiler.backend.FunctionNameMangling;
import io.katana.compiler.backend.llvm.FileCodegenContext;
import io.katana.compiler.backend.llvm.RValueToLValueConversion;
import io.katana.compiler.backend.llvm.ir.decl.IrFunctionBuilder;
import io.katana.compiler.backend.llvm.ir.instr.IrInstrConversion;
import io.katana.compiler.backend.llvm.ir.type.IrType;
import io.katana.compiler.backend.llvm.ir.type.IrTypeStructLiteral;
import io.katana.compiler.backend.llvm.ir.type.IrTypes;
import io.katana.compiler.backend.llvm.ir.value.IrValue;
import io.katana.compiler.backend.llvm.ir.value.IrValueSsa;
import io.katana.compiler.backend.llvm.ir.value.IrValues;
import io.katana.compiler.sema.decl.SemaDeclExternFunction;
import io.katana.compiler.sema.expr.*;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ExprLowerer implements IVisitor
{
	private static final int SLICE_POINTER_FIELD_INDEX = 0;
	private static final int SLICE_LENGTH_FIELD_INDEX = 1;

	private final FileCodegenContext context;
	private final IrFunctionBuilder builder;

	private ExprLowerer(FileCodegenContext context, IrFunctionBuilder builder)
	{
		this.context = context;
		this.builder = builder;
	}

	public static Maybe<IrValue> lower(SemaExpr expr, FileCodegenContext context, IrFunctionBuilder builder)
	{
		var visitor = new ExprLowerer(context, builder);
		return Maybe.wrap((IrValue)expr.accept(visitor));
	}

	private IrValue lower(SemaExpr expr)
	{
		return (IrValue)expr.accept(this);
	}

	private IrType lower(SemaType type)
	{
		return TypeLowerer.lower(type, context.platform());
	}

	private IrValue visit(RValueToLValueConversion conversion)
	{
		var value = lower(conversion.expr);
		var type = conversion.expr.type();
		var alignment = Types.alignof(type, context.platform());
		var typeIr = lower(type);
		var pointer = builder.alloca(typeIr, alignment);
		builder.store(typeIr, value, pointer);
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

	private IrValueSsa generateGetElementPtr(SemaExpr compoundExpr, int index)
	{
		return generateGetElementPtr(compoundExpr, IrTypes.I32, IrValues.ofConstant(index));
	}

	private IrValueSsa generateGetElementPtr(SemaExpr compoundExpr, IrType indexType, IrValue index)
	{
		var compound = lower(compoundExpr);
		var baseType = lower(Types.removePointer(compoundExpr.type()));
		return builder.getelementptr(baseType, compound, indexType, index);
	}

	private IrValue visit(SemaExprArrayAccess arrayAccess)
	{
		if(Types.isZeroSized(arrayAccess.type()))
			return null;

		var isRValue = arrayAccess.expr.kind() == ExprKind.RVALUE;

		if(isRValue)
			arrayAccess.expr = new RValueToLValueConversion(arrayAccess.expr);

		var indexType = lower(arrayAccess.index.type());
		var index = lower(arrayAccess.index);
		var result = generateGetElementPtr(arrayAccess.expr, indexType, index);

		if(isRValue)
		{
			var resultType = lower(arrayAccess.type());
			return builder.load(resultType, result);
		}

		return result;
	}

	private IrValue visit(SemaExprArrayGetLength arrayGetLength)
	{
		return IrValues.ofConstant(Types.arrayLength(arrayGetLength.expr.type()));
	}

	private IrValue visit(SemaExprArrayGetPointer arrayGetPointer)
	{
		return generateGetElementPtr(arrayGetPointer.expr, 0);
	}

	private IrValueSsa generateSliceConstruction(SemaType elementType, IrValue pointer, IrValue length)
	{
		var sliceType = (IrTypeStructLiteral)lower(Types.addSlice(elementType));
		var pointerType = sliceType.fields.get(0);
		var lengthType = sliceType.fields.get(1);

		var intermediate = builder.insertvalue(sliceType, IrValues.UNDEF, pointerType, pointer,
		                                       SLICE_POINTER_FIELD_INDEX);
		return builder.insertvalue(sliceType, intermediate, lengthType, length, SLICE_LENGTH_FIELD_INDEX);
	}

	private IrValue visit(SemaExprArrayGetSlice arrayGetSlice)
	{
		var elementType = Types.removeSlice(arrayGetSlice.type());
		var pointer = generateGetElementPtr(arrayGetSlice.expr, 0);
		var length = Types.arrayLength(Types.removePointer(arrayGetSlice.expr.type()));
		return generateSliceConstruction(elementType, pointer, IrValues.ofConstant(length));
	}

	private IrValue visit(SemaExprAssign assign)
	{
		if(Types.isZeroSized(assign.type()))
			return null;

		var type = lower(assign.right.type());
		var right = lower(assign.right);
		var left = lower(assign.left);
		builder.store(type, right, left);
		return left;
	}

	public IrValue visit(SemaExprBuiltinCall builtinCall)
	{
		var args = builtinCall.args.stream()
		                           .map(this::lower)
		                           .collect(Collectors.toList());

		var type = lower(builtinCall.args.get(0).type());
		return builder.binary(builtinCall.name, type, args.get(0), args.get(1));
	}

	private IrInstrConversion.Kind lowerCastKind(SemaExprCast.Kind kind, SemaType targetType)
	{
		switch(kind)
		{
		case WIDEN_CAST:
			if(Types.isFloatingPoint(targetType))
				return IrInstrConversion.Kind.FPEXT;

			if(Types.isSigned(targetType))
				return IrInstrConversion.Kind.SEXT;

			if(Types.isUnsigned(targetType))
				return IrInstrConversion.Kind.ZEXT;

			break;

		case NARROW_CAST:
			if(Types.isFloatingPoint(targetType))
				return IrInstrConversion.Kind.FPTRUNC;

			if(Types.isInteger(targetType))
				return IrInstrConversion.Kind.TRUNC;

			break;

		case POINTER_CAST:
			if(Types.isPointer(targetType))
				return IrInstrConversion.Kind.INTTOPTR;

			if(Types.isInteger(targetType))
				return IrInstrConversion.Kind.PTRTOINT;

			break;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private IrValueSsa generateCast(IrValue value, SemaType sourceType, SemaType targetType, SemaExprCast.Kind kind)
	{
		var kindIr = lowerCastKind(kind, targetType);
		var sourceTypeIr = lower(sourceType);
		var targetTypeIr = lower(targetType);
		return builder.convert(kindIr, sourceTypeIr, value, targetTypeIr);
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

	private IrValue generateFunctionCall(IrValue function, List<SemaExpr> args, SemaType returnType, Inlining inline)
	{
		var returnTypeIr = lower(returnType);

		var argsIr = args.stream()
		                 .map(this::lower)
		                 .collect(Collectors.toList());

		var argTypesIr = args.stream()
		                     .map(SemaExpr::type)
		                     .map(this::lower)
		                     .collect(Collectors.toList());

		return builder.call(returnTypeIr, function, argTypesIr, argsIr, inline).unwrap();
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
			name = FunctionNameMangling.of(call.function);

		var function = IrValues.ofSymbol(name);
		return generateFunctionCall(function, call.args, call.function.ret, call.inline);
	}

	private IrValue visit(SemaExprImplicitConversionArrayPointerToPointer conversion)
	{
		return generateGetElementPtr(conversion.expr, 0);
	}

	private IrValue visit(SemaExprImplicitConversionArrayPointerToByteSlice conversion)
	{
		var elementType = Types.removeArray(Types.removePointer(conversion.expr.type()));
		var length = Types.arrayLength(Types.removePointer(conversion.expr.type()));
		var size = length * Types.sizeof(elementType, context.platform());

		var pointerType = IrTypes.ofPointer(lower(elementType));
		var pointer = generateGetElementPtr(conversion.expr, 0);
		var bytePointerType = IrTypes.ofPointer(IrTypes.I8);
		var bytePointer = builder.convert(IrInstrConversion.Kind.BITCAST, pointerType, pointer, bytePointerType);

		return generateSliceConstruction(SemaTypeBuiltin.BYTE, bytePointer, IrValues.ofConstant(length));
	}

	private IrValue visit(SemaExprImplicitConversionArrayPointerToSlice conversion)
	{
		var pointer = generateGetElementPtr(conversion.expr, 0);
		var length = Types.arrayLength(Types.removePointer(conversion.expr.type()));
		var elementType = Types.removeSlice(conversion.type());
		return generateSliceConstruction(elementType, pointer, IrValues.ofConstant(length));
	}

	private IrValue visit(SemaExprImplicitConversionLValueToRValue conversion)
	{
		if(Types.isZeroSized(conversion.type()))
			return null;

		var value = lower(conversion.expr);
		var type = lower(conversion.type());
		return builder.load(type, value);
	}

	private IrValue visit(SemaExprImplicitConversionNonNullablePointerToNullablePointer conversion)
	{
		return lower(conversion.expr);
	}

	private IrValue visit(SemaExprImplicitConversionNullToSlice conversion)
	{
		return generateSliceConstruction(Types.removeSlice(conversion.type), IrValues.ADDRESS_ONE, IrValues.ofConstant(0));
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
		var sourceType = lower(conversion.expr.type());
		var value = lower(conversion.expr);
		var targetType = IrTypes.ofPointer(IrTypes.I8);
		return builder.convert(IrInstrConversion.Kind.BITCAST, sourceType, value, targetType);
	}

	private IrValue visit(SemaExprImplicitConversionSliceToByteSlice conversion)
	{
		var elementType = Types.removeSlice(conversion.expr.type());
		var slice = lower(conversion.expr);
		var sliceType = lower(conversion.expr.type());
		var pointer = builder.extractvalue(sliceType, slice, SLICE_POINTER_FIELD_INDEX);
		var pointerType = IrTypes.ofPointer(lower(elementType));
		var length = builder.extractvalue(sliceType, slice, SLICE_LENGTH_FIELD_INDEX);
		var lengthType = lower(SemaTypeBuiltin.INT);
		var sizeof = Types.sizeof(elementType, context.platform());
		var byteSize = builder.binary("mul", lengthType, length, IrValues.ofConstant(sizeof));
		var bytePointerType = IrTypes.ofPointer(IrTypes.I8);
		var bytePointer = builder.convert(IrInstrConversion.Kind.BITCAST, pointerType, pointer, bytePointerType);
		return generateSliceConstruction(SemaTypeBuiltin.BYTE, bytePointer, byteSize);
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

	private IrValue visit(SemaExprIndirectFunctionCall functionCall)
	{
		var function = lower(functionCall.expr);
		return generateFunctionCall(function, functionCall.args, functionCall.type(), Inlining.AUTO);
	}

	private IrValue visit(SemaExprFieldAccess fieldAccess)
	{
		var isRValue = fieldAccess.expr.kind() == ExprKind.RVALUE;

		if(isRValue)
			fieldAccess.expr = new RValueToLValueConversion(fieldAccess.expr);

		var result = generateGetElementPtr(fieldAccess.expr, fieldAccess.field.index);

		if(isRValue)
		{
			var fieldType = lower(fieldAccess.field.type);
			return builder.load(fieldType, result);
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

		var name = FunctionNameMangling.of(namedFunc.func);
		return IrValues.ofSymbol(name);
	}

	private IrValue visit(SemaExprNamedGlobal namedGlobal)
	{
		var name = namedGlobal.global.qualifiedName().toString();
		return IrValues.ofSymbol(name);
	}

	private IrValue visit(SemaExprNamedVar namedVar)
	{
		return IrValues.ofSsa(namedVar.variable.name);
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
		return builder.extractvalue(compoundType, compoundIr, index);
	}

	private IrValue visit(SemaExprSliceGetLength sliceGetLength)
	{
		if(sliceGetLength.expr.kind() == ExprKind.RVALUE)
			return generateExtractValue(sliceGetLength.expr, SLICE_LENGTH_FIELD_INDEX);

		return generateGetElementPtr(sliceGetLength.expr, SLICE_LENGTH_FIELD_INDEX);
	}

	private IrValue visit(SemaExprSliceGetPointer sliceGetPointer)
	{
		if(sliceGetPointer.expr.kind() == ExprKind.RVALUE)
			return generateExtractValue(sliceGetPointer.expr, SLICE_POINTER_FIELD_INDEX);

		return generateGetElementPtr(sliceGetPointer.expr, SLICE_POINTER_FIELD_INDEX);
	}
}
