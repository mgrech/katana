// Copyright 2016-2019 Markus Grech
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

package io.katana.compiler.backend.llvm.codegen;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.Inlining;
import io.katana.compiler.analysis.Types;
import io.katana.compiler.backend.FunctionNameMangling;
import io.katana.compiler.backend.llvm.FileCodegenContext;
import io.katana.compiler.backend.llvm.RValueToLValueConversion;
import io.katana.compiler.backend.llvm.ir.decl.IrFunctionBuilder;
import io.katana.compiler.backend.llvm.ir.instr.IrInstrConversion;
import io.katana.compiler.backend.llvm.ir.instr.IrInstrGetElementPtr;
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
import io.katana.compiler.utils.Fraction;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ExprCodegen extends IVisitor<IrValue>
{
	private static final int SLICE_POINTER_FIELD_INDEX = 0;
	private static final int SLICE_LENGTH_FIELD_INDEX = 1;

	private final FileCodegenContext context;
	private final IrFunctionBuilder builder;

	private ExprCodegen(FileCodegenContext context, IrFunctionBuilder builder)
	{
		this.context = context;
		this.builder = builder;
	}

	public static Maybe<IrValue> generate(SemaExpr expr, FileCodegenContext context, IrFunctionBuilder builder)
	{
		var codegen = new ExprCodegen(context, builder);
		return Maybe.wrap(codegen.generate(expr));
	}

	private IrValue generate(SemaExpr expr)
	{
		return invokeSelf(expr);
	}

	private IrType generate(SemaType type)
	{
		return TypeCodegen.generate(type, context.platform());
	}

	private IrValue visit(RValueToLValueConversion conversion)
	{
		var value = generate(conversion.expr);
		var type = conversion.expr.type();
		var alignment = Types.alignof(type, context.platform());
		var typeIr = generate(type);
		var pointer = builder.alloca(typeIr, alignment);
		builder.store(typeIr, value, pointer);
		return pointer;
	}

	private IrValue visit(SemaExprAddressof addressof)
	{
		if(Types.isZeroSized(addressof.pointeeExpr.type()))
			return IrValues.ADDRESS_ONE;

		return generate(addressof.pointeeExpr);
	}

	private IrValue visit(SemaExprAlignofExpr alignof)
	{
		var alignment = Types.alignof(alignof.nestedExpr.type(), context.platform());
		return IrValues.ofConstant(alignment);
	}

	private IrValue visit(SemaExprAlignofType alignof)
	{
		var alignment = Types.alignof(alignof.inspectedType, context.platform());
		return IrValues.ofConstant(alignment);
	}

	private IrValueSsa generateGetElementPtr(SemaExpr compoundExpr, boolean implicitIndexZero, int index)
	{
		return generateGetElementPtr(compoundExpr, implicitIndexZero, IrTypes.I32, IrValues.ofConstant(index));
	}

	private IrValueSsa generateGetElementPtr(SemaExpr compoundExpr, boolean implicitIndexZero, IrType indexType, IrValue index)
	{
		var compound = generate(compoundExpr);
		var baseType = generate(Types.removePointer(compoundExpr.type()));
		var indices = new ArrayList<IrInstrGetElementPtr.Index>();

		if(implicitIndexZero)
			indices.add(0, new IrInstrGetElementPtr.Index(IrTypes.I64, IrValues.ofConstant(0)));

		indices.add(new IrInstrGetElementPtr.Index(indexType, index));
		return builder.getelementptr(baseType, compound, indices);
	}

	private IrValue visit(SemaExprArrayIndexAccess arrayIndexAccess)
	{
		if(Types.isZeroSized(arrayIndexAccess.type()))
			return null;

		var isRValue = arrayIndexAccess.arrayExpr.kind() == ExprKind.RVALUE;

		if(isRValue)
			arrayIndexAccess.arrayExpr = new RValueToLValueConversion(arrayIndexAccess.arrayExpr);

		var indexType = generate(arrayIndexAccess.indexExpr.type());
		var index = generate(arrayIndexAccess.indexExpr);
		var result = generateGetElementPtr(arrayIndexAccess.arrayExpr, true, indexType, index);

		if(isRValue)
		{
			var resultType = generate(arrayIndexAccess.type());
			return builder.load(resultType, result);
		}

		return result;
	}

	private IrValue visit(SemaExprArrayGetLength arrayGetLength)
	{
		return IrValues.ofConstant(Types.arrayLength(arrayGetLength.arrayExpr.type()));
	}

	private IrValue visit(SemaExprArrayGetPointer arrayGetPointer)
	{
		return generateGetElementPtr(arrayGetPointer.arrayExpr, true, 0);
	}

	private IrValueSsa generateSliceConstruction(SemaType elementType, IrValue pointer, IrValue length)
	{
		var sliceType = (IrTypeStructLiteral)generate(Types.addSlice(elementType));
		var pointerType = sliceType.fields.get(0);
		var lengthType = sliceType.fields.get(1);

		var intermediate = builder.insertvalue(sliceType, IrValues.UNDEF, pointerType, pointer,
		                                       SLICE_POINTER_FIELD_INDEX);
		return builder.insertvalue(sliceType, intermediate, lengthType, length, SLICE_LENGTH_FIELD_INDEX);
	}

	private IrValue visit(SemaExprArrayGetSlice arrayGetSlice)
	{
		var elementType = Types.removeSlice(arrayGetSlice.type());
		var pointer = generateGetElementPtr(arrayGetSlice.arrayExpr, true, 0);
		var length = Types.arrayLength(Types.removePointer(arrayGetSlice.arrayExpr.type()));
		return generateSliceConstruction(elementType, pointer, IrValues.ofConstant(length));
	}

	private IrValue visit(SemaExprAssign assign)
	{
		if(Types.isZeroSized(assign.type()))
			return null;

		var type = generate(assign.right.type());
		var right = generate(assign.right);
		var left = generate(assign.left);
		builder.store(type, right, left);
		return left;
	}

	private static String prefixForType(SemaType type, boolean distinguishSignedness)
	{
		if(Types.isFloatingPoint(type))
			return "f";

		if(distinguishSignedness)
			return Types.isSigned(type) ? "s" : "u";

		return "";
	}

	private IrValue generateBinary(String name, List<SemaExpr> args, boolean distinguishSigned)
	{
		var type = args.get(0).type();
		var prefix = prefixForType(type, distinguishSigned);

		var argsIr = args.stream()
		                 .map(this::generate)
		                 .collect(Collectors.toList());

		var typeIr = generate(args.get(0).type());
		return builder.binary(prefix + name, typeIr, argsIr.get(0), argsIr.get(1));
	}

	private IrValue generateCompare(String comparison, List<SemaExpr> args)
	{
		var type = args.get(0).type();

		var instrPrefix = Types.isFloatingPoint(type) ? "f" : "i";
		var cmpPrefix = Types.isFloatingPoint(type)
		                ? "o"
		                : "eq".equals(comparison) || "ne".equals(comparison)
		                  ? ""
		                  : Types.isSigned(type)
		                    ? "s"
		                    : "u";
		var instr = String.format("%scmp %s%s", instrPrefix, cmpPrefix, comparison);

		var left = generate(args.get(0));
		var right = generate(args.get(1));
		return builder.binary(instr, generate(type), left, right);
	}

	private IrValue generateRotate(String which, List<SemaExpr> argExprs)
	{
		var leftExpr = argExprs.get(0);
		var leftIr = generate(leftExpr);
		var rightExpr = argExprs.get(1);
		var rightIr = generate(rightExpr);

		var typeIr = generate(leftExpr.type());
		var intrinsicName = IrValues.ofSymbol(String.format("llvm.%s.%s", which, typeIr));
		var argTypesIr = List.of(typeIr, typeIr, typeIr);
		var argsIr = List.of(leftIr, leftIr, rightIr);

		return builder.call(typeIr, intrinsicName, argTypesIr, argsIr, Inlining.AUTO).get();
	}

	private IrValue generateIntrinsicCall(SemaType returnType, String name, List<SemaExpr> argExprs)
	{
		var returnTypeIr = generate(returnType);
		var nameIr = IrValues.ofSymbol(name);

		var argTypesIr = argExprs.stream()
		                         .map(SemaExpr::type)
		                         .map(this::generate)
		                         .collect(Collectors.toList());

		var argsIr = argExprs.stream()
		                     .map(this::generate)
		                     .collect(Collectors.toList());

		return builder.call(returnTypeIr, nameIr, argTypesIr, argsIr, Inlining.AUTO).unwrap();
	}

	public IrValue visit(SemaExprBuiltinCall builtinCall)
	{
		switch(builtinCall.builtin)
		{
		case ADD: return generateBinary("add", builtinCall.argExprs, false);
		case SUB: return generateBinary("sub", builtinCall.argExprs, false);
		case MUL: return generateBinary("mul", builtinCall.argExprs, false);
		case DIV: return generateBinary("div", builtinCall.argExprs, true);
		case REM: return generateBinary("rem", builtinCall.argExprs, true);

		case DIV_POW2:
			{
				var type = builtinCall.returnType;
				var instr = Types.isSigned(type) ? "ashr" : "lshr";
				return generateBinary(instr, builtinCall.argExprs, false);
			}

		case NEG:
			{
				var type = ((SemaTypeBuiltin)builtinCall.returnType).which;
				var zero = type.kind == BuiltinType.Kind.INT
				           ? new SemaExprLitInt(BigInteger.ZERO, type)
				           : new SemaExprLitFloat(Fraction.of(0, 1), type);

				// llvm does not have an instruction for negation, use 0-x instead
				var argExprs = new ArrayList<>(builtinCall.argExprs);
				argExprs.add(0, zero);
				return generateBinary("sub", argExprs, false);
			}

		case CMP_EQ:  return generateCompare("eq", builtinCall.argExprs);
		case CMP_NEQ: return generateCompare("ne", builtinCall.argExprs);
		case CMP_LT:  return generateCompare("lt", builtinCall.argExprs);
		case CMP_LTE: return generateCompare("le", builtinCall.argExprs);
		case CMP_GT:  return generateCompare("gt", builtinCall.argExprs);
		case CMP_GTE: return generateCompare("ge", builtinCall.argExprs);

		case AND: return generateBinary("and",  builtinCall.argExprs, false);
		case OR:  return generateBinary("or",   builtinCall.argExprs, false);
		case XOR: return generateBinary("xor",  builtinCall.argExprs, false);
		case SHL: return generateBinary("shl",  builtinCall.argExprs, false);
		case SHR: return generateBinary("lshr", builtinCall.argExprs, false);

		case ROL: return generateRotate("fshl", builtinCall.argExprs);
		case ROR: return generateRotate("fshr", builtinCall.argExprs);

		case NOT:
			{
				var type = ((SemaTypeBuiltin)builtinCall.returnType).which;
				var neg1 = new SemaExprLitInt(BigInteger.valueOf(-1), type);

				// llvm does not have an instruction for bitwise not, use x^-1 instead
				var argExprs = new ArrayList<>(builtinCall.argExprs);
				argExprs.add(neg1);
				return generateBinary("xor", argExprs, false);
			}

		case CLZ:
			{
				var name = "llvm.ctlz." + generate(builtinCall.returnType);
				var argExprs = new ArrayList<>(builtinCall.argExprs);
				argExprs.add(SemaExprLitBool.FALSE); // undef when zero: false
				return generateIntrinsicCall(builtinCall.returnType, name, argExprs);
			}

		case CTZ:
			{

				var name = "llvm.cttz." + generate(builtinCall.returnType);
				var argExprs = new ArrayList<>(builtinCall.argExprs);
				argExprs.add(SemaExprLitBool.FALSE); // undef when zero: false
				return generateIntrinsicCall(builtinCall.returnType, name, argExprs);
			}

		case POPCNT:
			{
				var name = "llvm.ctpop." + generate(builtinCall.returnType);
				return generateIntrinsicCall(builtinCall.returnType, name, builtinCall.argExprs);
			}

		case BSWAP:
			{
				var name = "llvm.bswap." + generate(builtinCall.returnType);
				return generateIntrinsicCall(builtinCall.returnType, name, builtinCall.argExprs);
			}

		case MEMCPY:
			{
				var name = "llvm.memcpy.p0i8.p0i8." + generate(SemaTypeBuiltin.INT);
				var argExprs = new ArrayList<>(builtinCall.argExprs);
				argExprs.add(SemaExprLitBool.FALSE); // volatile: false
				return generateIntrinsicCall(builtinCall.returnType, name, argExprs);
			}

		case MEMMOVE:
			{
				var name = "llvm.memmove.p0i8.p0i8." + generate(SemaTypeBuiltin.INT);
				var argExprs = new ArrayList<>(builtinCall.argExprs);
				argExprs.add(SemaExprLitBool.FALSE); // volatile: false
				return generateIntrinsicCall(builtinCall.returnType, name, argExprs);
			}

		case MEMSET:
			{
				var name = "llvm.memset.p0i8." + generate(SemaTypeBuiltin.INT);
				var argExprs = new ArrayList<>(builtinCall.argExprs);
				argExprs.add(SemaExprLitBool.FALSE); // volatile: false
				return generateIntrinsicCall(builtinCall.returnType, name, argExprs);
			}

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private IrInstrConversion.Kind generateCastKind(SemaExprCast.Kind kind, SemaType targetType)
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
		var kindIr = generateCastKind(kind, targetType);
		var sourceTypeIr = generate(sourceType);
		var targetTypeIr = generate(targetType);
		return builder.convert(kindIr, sourceTypeIr, value, targetTypeIr);
	}

	private IrValue visit(SemaExprCast cast)
	{
		var value = generate(cast.nestedExpr);

		var sourceType = cast.nestedExpr.type();
		var targetType = cast.targetType;

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
		return generate(const_.nestedExpr);
	}

	private IrValue visit(SemaExprDeref deref)
	{
		if(Types.isZeroSized(deref.type()))
			return null;

		return generate(deref.pointerExpr);
	}

	private IrValue generateFunctionCall(IrValue function, List<SemaExpr> args, SemaType returnType, Inlining inline)
	{
		args = args.stream()
		           .filter(a -> !Types.isZeroSized(a.type()))
		           .collect(Collectors.toList());

		var returnTypeIr = generate(returnType);

		var argsIr = args.stream()
		                 .map(this::generate)
		                 .collect(Collectors.toList());

		var argTypesIr = args.stream()
		                     .map(SemaExpr::type)
		                     .map(this::generate)
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
		return generateFunctionCall(function, call.args, call.function.returnType, call.inline);
	}

	private IrValue visit(SemaExprImplicitConversionArrayPointerToPointer conversion)
	{
		return generateGetElementPtr(conversion.nestedExpr, true, 0);
	}

	private IrValue visit(SemaExprImplicitConversionArrayPointerToByteSlice conversion)
	{
		var elementType = Types.removeArray(Types.removePointer(conversion.nestedExpr.type()));
		var length = Types.arrayLength(Types.removePointer(conversion.nestedExpr.type()));
		var size = length * Types.sizeof(elementType, context.platform());

		var pointerType = IrTypes.ofPointer(generate(elementType));
		var pointer = generateGetElementPtr(conversion.nestedExpr, true, 0);
		var bytePointerType = IrTypes.ofPointer(IrTypes.I8);
		var bytePointer = builder.convert(IrInstrConversion.Kind.BITCAST, pointerType, pointer, bytePointerType);

		return generateSliceConstruction(SemaTypeBuiltin.BYTE, bytePointer, IrValues.ofConstant(length));
	}

	private IrValue visit(SemaExprImplicitConversionArrayPointerToSlice conversion)
	{
		var pointer = generateGetElementPtr(conversion.nestedExpr, true, 0);
		var length = Types.arrayLength(Types.removePointer(conversion.nestedExpr.type()));
		var elementType = Types.removeSlice(conversion.type());
		return generateSliceConstruction(elementType, pointer, IrValues.ofConstant(length));
	}

	private IrValue visit(SemaExprImplicitConversionLValueToRValue conversion)
	{
		if(Types.isZeroSized(conversion.type()))
			return null;

		var value = generate(conversion.nestedExpr);
		var type = generate(conversion.type());
		return builder.load(type, value);
	}

	private IrValue visit(SemaExprImplicitConversionNonNullablePointerToNullablePointer conversion)
	{
		return generate(conversion.nestedExpr);
	}

	private IrValue visit(SemaExprImplicitConversionNullToSlice conversion)
	{
		return generateSliceConstruction(Types.removeSlice(conversion.targetType), IrValues.ADDRESS_ONE, IrValues.ofConstant(0));
	}

	private IrValue visit(SemaExprImplicitConversionNullToNullablePointer conversion)
	{
		return IrValues.NULL;
	}

	private IrValue visit(SemaExprImplicitConversionPointerToNonConstToPointerToConst conversion)
	{
		return generate(conversion.nestedExpr);
	}

	private IrValue visit(SemaExprImplicitConversionPointerToBytePointer conversion)
	{
		var sourceType = generate(conversion.nestedExpr.type());
		var value = generate(conversion.nestedExpr);
		var targetType = IrTypes.ofPointer(IrTypes.I8);
		return builder.convert(IrInstrConversion.Kind.BITCAST, sourceType, value, targetType);
	}

	private IrValue visit(SemaExprImplicitConversionSliceToByteSlice conversion)
	{
		var elementType = Types.removeSlice(conversion.nestedExpr.type());
		var slice = generate(conversion.nestedExpr);
		var sliceType = generate(conversion.nestedExpr.type());
		var pointer = builder.extractvalue(sliceType, slice, SLICE_POINTER_FIELD_INDEX);
		var pointerType = IrTypes.ofPointer(generate(elementType));
		var length = builder.extractvalue(sliceType, slice, SLICE_LENGTH_FIELD_INDEX);
		var lengthType = generate(SemaTypeBuiltin.INT);
		var sizeof = Types.sizeof(elementType, context.platform());
		var byteSize = builder.binary("mul", lengthType, length, IrValues.ofConstant(sizeof));
		var bytePointerType = IrTypes.ofPointer(IrTypes.I8);
		var bytePointer = builder.convert(IrInstrConversion.Kind.BITCAST, pointerType, pointer, bytePointerType);
		return generateSliceConstruction(SemaTypeBuiltin.BYTE, bytePointer, byteSize);
	}

	private IrValue visit(SemaExprImplicitConversionSliceToSliceOfConst conversion)
	{
		return generate(conversion.nestedExpr);
	}

	private IrValue visit(SemaExprImplicitConversionWiden conversion)
	{
		var value = generate(conversion.nestedExpr);
		var sourceType = conversion.nestedExpr.type();
		var targetType = conversion.type();
		return generateCast(value, sourceType, targetType, SemaExprCast.Kind.WIDEN_CAST);
	}

	private IrValue visit(SemaExprIndirectFunctionCall functionCall)
	{
		var function = generate(functionCall.functionExpr);
		return generateFunctionCall(function, functionCall.argExprs, functionCall.type(), Inlining.AUTO);
	}

	private IrValue visit(SemaExprFieldAccess fieldAccess)
	{
		var isRValue = fieldAccess.structExpr.kind() == ExprKind.RVALUE;

		if(isRValue)
			fieldAccess.structExpr = new RValueToLValueConversion(fieldAccess.structExpr);

		var result = generateGetElementPtr(fieldAccess.structExpr, true, fieldAccess.field.index);

		if(isRValue)
		{
			var fieldType = generate(fieldAccess.field.type);
			return builder.load(fieldType, result);
		}

		return result;
	}

	private IrValue visit(SemaExprLitArray lit)
	{
		var elementType = generate(lit.elementType);
		var values = lit.elementExprs.stream()
		                             .map(this::generate)
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
		if(namedFunc.decl instanceof SemaDeclExternFunction)
		{
			var name = ((SemaDeclExternFunction)namedFunc.decl).externName.or(namedFunc.decl.name());
			return IrValues.ofSymbol(name);
		}

		var name = FunctionNameMangling.of(namedFunc.decl);
		return IrValues.ofSymbol(name);
	}

	private IrValue visit(SemaExprNamedGlobal namedGlobal)
	{
		var name = namedGlobal.decl.qualifiedName().toString();
		return IrValues.ofSymbol(name);
	}

	private IrValue visit(SemaExprNamedVar namedVar)
	{
		return IrValues.ofSsa(namedVar.decl.name);
	}

	private IrValue visit(SemaExprNamedParam namedParam)
	{
		return IrValues.ofSsa(namedParam.decl.name);
	}

	private IrValue visit(SemaExprOffsetof offsetof)
	{
		var offset = offsetof.field.offsetof();
		return IrValues.ofConstant(offset);
	}

	private IrValue visit(SemaExprSizeofExpr sizeof)
	{
		var size = Types.sizeof(sizeof.nestedExpr.type(), context.platform());
		return IrValues.ofConstant(size);
	}

	private IrValue visit(SemaExprSizeofType sizeof)
	{
		var size = Types.sizeof(sizeof.inspectedType, context.platform());
		return IrValues.ofConstant(size);
	}

	private IrValueSsa generateExtractValue(SemaExpr compound, int index)
	{
		var compoundIr = generate(compound);
		var compoundType = generate(compound.type());
		return builder.extractvalue(compoundType, compoundIr, index);
	}

	private IrValue visit(SemaExprSliceGetLength sliceGetLength)
	{
		if(sliceGetLength.sliceExpr.kind() == ExprKind.RVALUE)
			return generateExtractValue(sliceGetLength.sliceExpr, SLICE_LENGTH_FIELD_INDEX);

		return generateGetElementPtr(sliceGetLength.sliceExpr, true, SLICE_LENGTH_FIELD_INDEX);
	}

	private IrValue visit(SemaExprSliceGetPointer sliceGetPointer)
	{
		if(sliceGetPointer.sliceExpr.kind() == ExprKind.RVALUE)
			return generateExtractValue(sliceGetPointer.sliceExpr, SLICE_POINTER_FIELD_INDEX);

		return generateGetElementPtr(sliceGetPointer.sliceExpr, true, SLICE_POINTER_FIELD_INDEX);
	}

	private IrValue visit(SemaExprSliceIndexAccess sliceIndexAccess)
	{
		var pointer = new SemaExprSliceGetPointer(sliceIndexAccess.sliceExpr).asRValue();
		var indexType = generate(sliceIndexAccess.indexExpr.type());
		var index = generate(sliceIndexAccess.indexExpr);
		return generateGetElementPtr(pointer, false, indexType, index);
	}
}
