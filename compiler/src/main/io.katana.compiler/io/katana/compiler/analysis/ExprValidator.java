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

package io.katana.compiler.analysis;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.ExportKind;
import io.katana.compiler.Inlining;
import io.katana.compiler.Limits;
import io.katana.compiler.ast.expr.*;
import io.katana.compiler.ast.type.AstType;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.TypeString;
import io.katana.compiler.op.*;
import io.katana.compiler.sema.decl.*;
import io.katana.compiler.sema.expr.*;
import io.katana.compiler.sema.scope.SemaScope;
import io.katana.compiler.sema.type.*;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ExprValidator extends IVisitor<SemaExpr>
{
	private SemaScope scope;
	private PlatformContext context;
	private Consumer<SemaDecl> validateDecl;

	private ExprValidator(SemaScope scope, PlatformContext context, Consumer<SemaDecl> validateDecl)
	{
		this.scope = scope;
		this.context = context;
		this.validateDecl = validateDecl;
	}

	public static SemaExpr validate(AstExpr expr, SemaScope scope, PlatformContext context, Consumer<SemaDecl> validateDecl, Maybe<SemaType> expectedType)
	{
		var validator = new ExprValidator(scope, context, validateDecl);
		return validator.validate(expr, expectedType.unwrap());
	}

	private SemaExpr validate(AstExpr expr)
	{
		return validate(expr, null);
	}

	private SemaExpr validate(AstExpr expr, SemaType expectedType)
	{
		var result = invokeSelf(expr, expectedType);

		if(expectedType == null)
			return result;

		return ImplicitConversions.perform(result, expectedType);
	}

	private SemaType validate(AstType type)
	{
		return TypeValidator.validate(type, scope, context, validateDecl);
	}

	private SemaExpr visit(AstExprProxy proxy, SemaType expectedType)
	{
		return validate(proxy.nestedExpr, expectedType);
	}

	private void checkArguments(List<SemaType> params, List<SemaExpr> args)
	{
		var expected = params.size();
		var actual = args.size();

		if(actual != expected)
		{
			var fmt = "invalid number of arguments: expected %s, %s given";
			throw new CompileException(String.format(fmt, expected, actual));
		}

		var it1 = params.iterator();
		var it2 = args.iterator();

		for(var argCount = 1; it1.hasNext(); ++argCount)
		{
			var paramType = it1.next();
			var paramTypeNoConst = Types.removeConst(paramType);
			var argType = it2.next().type();
			var argTypeNoConst = Types.removeConst(argType);

			if(!Types.equal(paramTypeNoConst, argTypeNoConst))
			{
				var fmt = "type mismatch in argument %s: expected '%s', got '%s'";
				throw new CompileException(String.format(fmt, argCount, TypeString.of(paramTypeNoConst), TypeString.of(argTypeNoConst)));
			}
		}
	}

	private SemaExpr visit(AstExprAddressof addressof, SemaType expectedType)
	{
		var expr = validate(addressof.pointeeExpr);

		if(expr instanceof SemaExprNamedOverloadSet)
		{
			var set = ((SemaExprNamedOverloadSet)expr).decl;

			if(set.overloads.size() > 1)
				throw new CompileException(String.format("cannot take address of function '%s': function is overloaded", set.qualifiedName()));

			expr = new SemaExprNamedFunc(set.overloads.get(0));
		}

		if(expr.kind() != ExprKind.LVALUE)
			throw new CompileException("address-of operator ('prefix &') requires lvalue operand");

		return new SemaExprAddressof(expr);
	}

	private SemaExpr visit(AstExprAlignofExpr alignof, SemaType expectedType)
	{
		return new SemaExprAlignofExpr(validate(alignof.nestedExpr));
	}

	private SemaExpr visit(AstExprAlignofType alignof, SemaType expectedType)
	{
		return new SemaExprAlignofType(validate(alignof.inspectedType));
	}

	private SemaExpr visit(AstExprIndexAccess indexAccess, SemaType expectedType)
	{
		var value = validate(indexAccess.indexeeExpr);
		var index = validate(indexAccess.indexExpr, SemaTypeBuiltin.INT);
		var indexType = index.type();

		value = autoDeref(value);

		if(!Types.isBuiltin(indexType, BuiltinType.INT))
			throw new CompileException(String.format("index access requires index of type 'int', got '%s'", TypeString.of(indexType)));

		if(!Types.isArray(value.type()) && !Types.isSlice(value.type()))
			throw new CompileException(String.format("index access requires expression yielding array or slice type, got '%s'", TypeString.of(value.type())));

		if(index.kind() == ExprKind.LVALUE)
			index = new SemaExprImplicitConversionLValueToRValue(index);

		if(Types.isSlice(value.type()))
			return new SemaExprSliceIndexAccess(value, index);

		return new SemaExprArrayIndexAccess(value, index);
	}

	private SemaExpr visit(AstExprAssign assign, SemaType expectedType)
	{
		var left = validate(assign.leftExpr);

		var leftType = left.type();
		var leftTypeNoConst = Types.removeConst(leftType);

		var right = validate(assign.rightExpr, leftTypeNoConst);

		if(Types.isConst(leftType) || left.kind() != ExprKind.LVALUE)
			throw new CompileException("non-const lvalue required on left side of assignment");

		if(leftType instanceof SemaTypeFunction)
			throw new CompileException("cannot assign to value of function type");

		var rightTypeNoConst = Types.removeConst(right.type());

		if(!Types.equal(leftTypeNoConst, rightTypeNoConst))
		{
			var fmt = "compatible types expected in assignment, got '%s' and '%s'";
			throw new CompileException(String.format(fmt, TypeString.of(leftTypeNoConst), TypeString.of(rightTypeNoConst)));
		}

		if(right.kind() == ExprKind.LVALUE)
			right = new SemaExprImplicitConversionLValueToRValue(right);

		return new SemaExprAssign(left, right);
	}

	private SemaExpr visit(AstExprBuiltinCall builtinCall, SemaType expectedType)
	{
		var maybeFunc = Builtins.tryFind(builtinCall.name);

		if(maybeFunc.isNone())
			throw new CompileException(String.format("builtin '%s' not found", builtinCall.name));

		var args = new ArrayList<SemaExpr>();

		for(var i = 0; i != builtinCall.args.size(); ++i)
		{
			var semaExpr = validate(builtinCall.args.get(i));
			var type = semaExpr.type();

			if(Types.isVoid(type))
			{
				var fmt = "expression passed to builtin '%s' as argument %s yields 'void'";
				throw new CompileException(String.format(fmt, builtinCall.name, i + 1));
			}

			if(semaExpr.kind() == ExprKind.LVALUE)
				semaExpr = new SemaExprImplicitConversionLValueToRValue(semaExpr);

			args.add(semaExpr);
		}

		var builtin = maybeFunc.unwrap();

		var argTypes = args.stream()
		                   .map(SemaExpr::type)
		                   .collect(Collectors.toList());
		var returnType = builtin.validateCall(argTypes);

		if(returnType.isNone())
			throw new CompileException(String.format("invalid arguments to builtin '%s'", builtin.which.sourceName));

		return new SemaExprBuiltinCall(builtin.which, args, returnType.unwrap());
	}

	private SemaExpr visit(AstExprConst const_, SemaType expectedType)
	{
		var expr = validate(const_.nestedExpr, expectedType);

		if(Types.isFunction(expr.type()))
			throw new CompileException("const symbol applied to value of function type");

		return new SemaExprConst(expr);
	}

	private SemaExpr visit(AstExprDeref deref, SemaType expectedType)
	{
		var expectedPointerType = expectedType == null ? null : Types.addNonNullablePointer(expectedType);
		var expr = validate(deref.pointerExpr, expectedPointerType);

		if(!Types.isPointer(expr.type()))
		{
			var fmt = "expected expression of pointer type in dereference operator ('prefix *'), got '%s'";
			throw new CompileException(String.format(fmt, TypeString.of(expr.type())));
		}

		if(expr.kind() == ExprKind.LVALUE)
			expr = new SemaExprImplicitConversionLValueToRValue(expr);

		return new SemaExprDeref(expr);
	}

	private SemaExpr visit(AstExprFunctionCall call, SemaType expectedType)
	{
		var expr = validate(call.functionExpr);

		if(expr instanceof SemaExprNamedImportedOverloadSet)
		{
			var set = ((SemaExprNamedImportedOverloadSet)expr).decl.overloadSet;

			var candidates = new ArrayList<SemaDeclFunction>();

			for(var function : set.overloads)
				if(function.exportKind != ExportKind.HIDDEN)
					candidates.add(function);

			return Overloading.resolve(candidates, set.name(), call.argExprs, call.inline, this::validate);
		}

		if(expr instanceof SemaExprNamedOverloadSet)
		{
			var set = ((SemaExprNamedOverloadSet)expr).decl;
			return Overloading.resolve(set.overloads, set.name(), call.argExprs, call.inline, this::validate);
		}

		expr = autoDeref(expr);

		if(!Types.isFunction(expr.type()))
			throw new CompileException("left side of function call does not yield a function type");

		var ftype = (SemaTypeFunction)expr.type();
		var args = new ArrayList<SemaExpr>();

		for(var i = 0; i != call.argExprs.size(); ++i)
		{
			var arg = call.argExprs.get(i);
			var type = ftype.paramTypes.get(i);

			var semaArg = validate(call.argExprs.get(i), type);

			if(semaArg.kind() == ExprKind.LVALUE)
				semaArg = new SemaExprImplicitConversionLValueToRValue(semaArg);

			args.add(semaArg);
		}

		checkArguments(ftype.paramTypes, args);

		return new SemaExprIndirectFunctionCall(expr, args);
	}

	private SemaExpr visit(AstExprLitArray lit, SemaType expectedType)
	{
		var maybeType = lit.elementType.map(this::validate);

		if(expectedType instanceof SemaTypeArray)
		{
			var array = (SemaTypeArray)expectedType;

			if(maybeType.isNone())
				maybeType = Maybe.some(array.elementType);
		}

		if(maybeType.isNone())
			throw new CompileException("element type of array literal could not be deduced");

		var type = maybeType.unwrap();
		var typeNoConst = Types.removeConst(type);

		var values = new ArrayList<SemaExpr>();

		for(var i = 0; i != lit.elementExprs.size(); ++i)
		{
			var semaExpr = validate(lit.elementExprs.get(i), type);
			var elemTypeNoConst = Types.removeConst(semaExpr.type());

			if(!Types.equal(elemTypeNoConst, typeNoConst))
			{
				var fmt = "element in array literal at index %s has type '%s', expected '%s'";
				throw new CompileException(String.format(fmt, i, TypeString.of(elemTypeNoConst), TypeString.of(typeNoConst)));
			}

			values.add(semaExpr);
		}

		return new SemaExprLitArray(maybeType.unwrap(), values);
	}

	private SemaExpr visit(AstExprLitBool lit, SemaType expectedType)
	{
		return SemaExprLitBool.of(lit.value);
	}

	private void errorLiteralTypeDeduction()
	{
		throw new CompileException("type of literal could not be deduced");
	}

	private BuiltinType deduceLiteralType(Maybe<SemaType> maybeType, boolean floatingPoint)
	{
		if(maybeType.isNone())
			errorLiteralTypeDeduction();

		var type = Types.removeConst(maybeType.get());

		if(!Types.isBuiltin(type))
			errorLiteralTypeDeduction();

		var builtin = (SemaTypeBuiltin)type;

		if(floatingPoint)
		{
			if(builtin.which.kind != BuiltinType.Kind.FLOAT)
				errorLiteralTypeDeduction();
		}
		else
		{
			if(builtin.which.kind != BuiltinType.Kind.INT && builtin.which.kind != BuiltinType.Kind.UINT)
				errorLiteralTypeDeduction();
		}

		return builtin.which;
	}

	private SemaExpr visit(AstExprLitFloat lit, SemaType expectedType)
	{
		var type = lit.type;

		if(type.isNone())
			type = Maybe.some(deduceLiteralType(Maybe.some(expectedType), true));

		if(lit.value == null)
			return new SemaExprLitFloat(null, type.unwrap());

		if(!Limits.inRange(lit.value, type.unwrap()))
			throw new CompileException(String.format("floating point literal value is out of range: %s", lit.value));

		return new SemaExprLitFloat(lit.value, type.unwrap());
	}

	private SemaExpr visit(AstExprLitInt lit, SemaType expectedType)
	{
		var type = lit.type;

		if(type.isNone())
			type = Maybe.some(deduceLiteralType(Maybe.some(expectedType), false));

		if(lit.value == null)
			return new SemaExprLitInt(null, type.unwrap());

		if(!Limits.inRange(lit.value, type.unwrap(), context))
			throw new CompileException(String.format("integer literal value is out of range: %s", lit.value));

		return new SemaExprLitInt(lit.value, type.unwrap());
	}

	private SemaExpr visit(AstExprLitNull lit, SemaType expectedType)
	{
		return SemaExprLitNull.INSTANCE;
	}

	private SemaExpr visit(AstExprLitString lit, SemaType expectedType)
	{
		return new SemaExprLitString(lit.value);
	}

	private SemaExpr namedDeclExpr(SemaDecl decl, boolean globalAccess)
	{
		validateDecl.accept(decl);

		if(decl instanceof SemaDeclGlobal)
		{
			if(!globalAccess)
				throw new CompileException("reference to global requires 'global' keyword");

			return new SemaExprNamedGlobal((SemaDeclGlobal)decl);
		}

		if(globalAccess)
			throw new CompileException("'global' keyword used on reference to symbol that is not a global");

		if(decl instanceof SemaDeclOverloadSet)
			return new SemaExprNamedOverloadSet((SemaDeclOverloadSet)decl);

		if(decl instanceof SemaDeclImportedOverloadSet)
			return new SemaExprNamedImportedOverloadSet((SemaDeclImportedOverloadSet)decl);

		throw new AssertionError("unreachable");
	}

	private void errorNoSuchField(SemaType type, String fieldName)
	{
		throw new CompileException(String.format("type '%s' has no field named '%s'", TypeString.of(type), fieldName));
	}

	private SemaExpr autoDeref(SemaExpr expr)
	{
		var type = expr.type();

		while(Types.isPointer(type))
		{
			if(expr.kind() == ExprKind.LVALUE)
				expr = new SemaExprImplicitConversionLValueToRValue(expr);

			expr = new SemaExprDeref(expr);
			type = expr.type();
		}

		return expr;
	}

	private SemaExpr visit(AstExprMemberAccess memberAccess, SemaType expectedType)
	{
		var expr = validate(memberAccess.accesseeExpr);

		if(expr instanceof SemaExprNamedRenamedImport)
		{
			var import_ = ((SemaExprNamedRenamedImport)expr).decl;
			var decl = import_.decls.get(memberAccess.memberName);

			if(decl == null)
				throw new CompileException(String.format("reference to unknown symbol '%s.%s'", import_.module.path(), memberAccess.memberName));

			return namedDeclExpr(decl, memberAccess.globalAccess);
		}

		expr = autoDeref(expr);

		var type = expr.type();
		var typeNoConst = Types.removeConst(type);

		if(typeNoConst instanceof SemaTypeSlice)
		{
			switch(memberAccess.memberName)
			{
			case "pointer": return new SemaExprSliceGetPointer(expr);
			case "length":  return new SemaExprSliceGetLength(expr);
			case "sliceof": return expr;
			default: errorNoSuchField(type, memberAccess.memberName);
			}
		}

		if(typeNoConst instanceof SemaTypeArray)
		{
			switch(memberAccess.memberName)
			{
			case "pointer":
				if(expr.kind() != ExprKind.LVALUE)
					throw new CompileException("cannot take pointer of array rvalue");

				return new SemaExprArrayGetPointer(expr);

			case "length": return new SemaExprArrayGetLength(expr);

			case "sliceof":
				if(expr.kind() != ExprKind.LVALUE)
					throw new CompileException("cannot take slice of array rvalue");

				return new SemaExprArrayGetSlice(expr);

			default: errorNoSuchField(type, memberAccess.memberName);
			}
		}

		if(!(typeNoConst instanceof SemaTypeStruct))
			errorNoSuchField(type, memberAccess.memberName);

		var struct = ((SemaTypeStruct)typeNoConst).decl;
		var field = struct.findField(memberAccess.memberName);

		if(field.isNone())
			errorNoSuchField(type, memberAccess.memberName);

		return new SemaExprFieldAccess(expr, field.unwrap(), Types.isConst(type));
	}

	private SemaExpr visit(AstExprNamedGlobal namedGlobal, SemaType expectedType)
	{
		var candidates = scope.find(namedGlobal.name);

		if(candidates.isEmpty())
			throw new CompileException(String.format("reference to unknown symbol '%s'", namedGlobal.name));

		if(candidates.size() > 1)
			throw new CompileException(String.format("ambiguos reference to symbol '%s'", namedGlobal.name));

		var symbol = candidates.get(0);

		if(!(symbol instanceof SemaDeclGlobal))
			throw new CompileException(String.format("symbol '%s' does not refer to a global", namedGlobal.name));

		return namedDeclExpr((SemaDecl)symbol, true);
	}

	private SemaExpr visit(AstExprNamedSymbol namedSymbol, SemaType expectedType)
	{
		var candidates = scope.find(namedSymbol.name);

		if(candidates.isEmpty())
			throw new CompileException(String.format("reference to unknown symbol '%s'", namedSymbol.name));

		if(candidates.size() > 1)
			throw new CompileException(String.format("ambiguous reference to symbol '%s'", namedSymbol.name));

		var symbol = candidates.get(0);

		if(symbol instanceof SemaDeclRenamedImport)
			return new SemaExprNamedRenamedImport((SemaDeclRenamedImport)symbol);

		if(symbol instanceof SemaDecl)
			return namedDeclExpr((SemaDecl)symbol, false);

		if(symbol instanceof SemaDeclFunctionDef.Variable)
			return new SemaExprNamedVar((SemaDeclFunctionDef.Variable)symbol);

		if(symbol instanceof SemaDeclFunction.Param)
			return new SemaExprNamedParam((SemaDeclFunction.Param)symbol);

		throw new CompileException(String.format("symbol '%s' does not refer to a value", namedSymbol.name));
	}

	private SemaExpr validateCast(AstType type, AstExpr expr, SemaExprCast.Kind kind)
	{
		var targetType = validate(type);
		var semaExpr = validate(expr);
		var sourceType = semaExpr.type();

		if(!CastValidator.isValidCast(sourceType, targetType, kind, context))
		{
			var fmt = "%s from expression of type '%s' to type '%s' is not valid";
			throw new CompileException(String.format(fmt, kind.toString().toLowerCase(), TypeString.of(sourceType), TypeString.of(targetType)));
		}

		if(semaExpr.kind() == ExprKind.LVALUE)
			semaExpr = new SemaExprImplicitConversionLValueToRValue(semaExpr);

		return new SemaExprCast(targetType, semaExpr, kind);
	}

	private SemaExpr visit(AstExprNarrowCast cast, SemaType expectedType)
	{
		return validateCast(cast.targetType, cast.nestedExpr, SemaExprCast.Kind.NARROW_CAST);
	}

	private SemaExpr visit(AstExprOffsetof offsetof, SemaType expectedType)
	{
		var candidates = scope.find(offsetof.typeName);

		if(candidates.isEmpty())
			throw new CompileException(String.format("use of unknown type '%s'", offsetof.typeName));

		if(candidates.size() > 1)
			throw new CompileException(String.format("ambiguous reference to symbol '%s'", offsetof.typeName));

		var symbol = candidates.get(0);

		if(!(symbol instanceof SemaDeclStruct))
			throw new CompileException(String.format("symbol '%s' does not refer to a type", offsetof.typeName));

		var field = ((SemaDeclStruct)symbol).findField(offsetof.fieldName);

		if(field.isNone())
			errorNoSuchField(new SemaTypeStruct((SemaDeclStruct)symbol), offsetof.fieldName);

		return new SemaExprOffsetof(field.unwrap());
	}

	private SemaExpr handleOperatorCall(String op, Kind kind, List<AstExpr> args, SemaType expectedType)
	{
		var candidates = scope.find(Operator.implName(op, kind));

		if(candidates.isEmpty())
			throw new CompileException(String.format("no implementation of operator '%s' found", op));

		if(candidates.size() > 1)
			throw new CompileException("nyi");

		var symbol = candidates.get(0);
		var set = symbol instanceof SemaDeclOverloadSet
		          ? (SemaDeclOverloadSet)symbol
		          : ((SemaDeclImportedOverloadSet)symbol).overloadSet;

		var overloads = set.overloads.stream()
		                             .filter(o -> o.exportKind != ExportKind.HIDDEN)
		                             .collect(Collectors.toList());

		return Overloading.resolve(overloads, set.name(), args, Inlining.AUTO, this::validate);
	}

	private SemaExpr visit(AstExprOpInfix op, SemaType expectedType)
	{
		return handleOperatorCall(op.decl.operator.symbol, Kind.INFIX, Arrays.asList(op.left, op.right), expectedType);
	}

	private SemaExpr visit(AstExprOpPrefix op, SemaType expectedType)
	{
		return handleOperatorCall(op.decl.operator.symbol, Kind.PREFIX, Collections.singletonList(op.expr), expectedType);
	}

	private SemaExpr visit(AstExprOpPostfix op, SemaType expectedType)
	{
		return handleOperatorCall(op.decl.operator.symbol, Kind.POSTFIX, Collections.singletonList(op.expr), expectedType);
	}

	private SemaExpr visit(AstExprParens parens, SemaType expectedType)
	{
		return validate(parens.nestedExpr, expectedType);
	}

	private SemaExpr visit(AstExprPointerCast cast, SemaType expectedType)
	{
		return validateCast(cast.targetType, cast.nestedExpr, SemaExprCast.Kind.POINTER_CAST);
	}

	private SemaExpr visit(AstExprSignCast cast, SemaType expectedType)
	{
		return validateCast(cast.targetType, cast.nestedExpr, SemaExprCast.Kind.SIGN_CAST);
	}

	private SemaExpr visit(AstExprSizeofExpr sizeof, SemaType expectedType)
	{
		return new SemaExprSizeofExpr(validate(sizeof.nestedExpr));
	}

	private SemaExpr visit(AstExprSizeofType sizeof, SemaType expectedType)
	{
		return new SemaExprSizeofType(validate(sizeof.inspectedType));
	}

	private SemaExpr visit(AstExprWidenCast cast, SemaType expectedType)
	{
		return validateCast(cast.targetType, cast.nestedExpr, SemaExprCast.Kind.WIDEN_CAST);
	}
}
