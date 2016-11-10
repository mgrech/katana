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

package katana.analysis;

import katana.BuiltinFunc;
import katana.BuiltinType;
import katana.Limits;
import katana.ast.expr.*;
import katana.backend.PlatformContext;
import katana.diag.TypeString;
import katana.op.*;
import katana.sema.SemaSymbol;
import katana.sema.decl.*;
import katana.sema.expr.*;
import katana.sema.scope.SemaScope;
import katana.sema.type.*;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ExprValidator implements IVisitor
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

	public static SemaExpr validate(AstExpr expr, SemaScope scope, PlatformContext context, Consumer<SemaDecl> validateDecl, Maybe<SemaType> deduce)
	{
		ExprValidator validator = new ExprValidator(scope, context, validateDecl);
		return (SemaExpr)expr.accept(validator, deduce);
	}

	private SemaExpr visit(AstExprProxy proxy, Maybe<SemaType> deduce)
	{
		return (SemaExpr)proxy.expr.accept(this, deduce);
	}

	private void checkArguments(List<SemaType> params, List<SemaExpr> args)
	{
		int expected = params.size();
		int actual = args.size();

		if(actual != expected)
		{
			String fmt = "invalid number of arguments: expected %s, %s given";
			throw new RuntimeException(String.format(fmt, expected, actual));
		}

		Iterator<SemaType> it1 = params.iterator();
		Iterator<SemaExpr> it2 = args.iterator();

		for(int argCount = 1; it1.hasNext(); ++argCount)
		{
			SemaType paramType = it1.next();
			SemaType argType = it2.next().type();

			if(TypeHelper.isVoidType(argType))
				throw new RuntimeException(String.format("expression given in argument %s yields 'void'", argCount));

			SemaType paramTypeDecayed = TypeHelper.decay(paramType);
			SemaType argTypeDecayed = TypeHelper.decay(argType);

			if(!SemaType.same(paramTypeDecayed, argTypeDecayed))
			{
				String fmt = "type mismatch in argument %s: expected '%s', got '%s'";
				throw new RuntimeException(String.format(fmt, argCount, TypeString.of(paramTypeDecayed), TypeString.of(argTypeDecayed)));
			}
		}
	}

	private SemaExpr visit(AstExprAddressof addressof, Maybe<SemaType> deduce)
	{
		SemaExpr expr = validate(addressof.expr, scope, context, validateDecl, Maybe.none());

		if(!(expr instanceof SemaExprLValueExpr))
			throw new RuntimeException("'addressof' requires lvalue operand");

		SemaExprLValueExpr lvalue = (SemaExprLValueExpr)expr;
		lvalue.useAsLValue(true);
		return new SemaExprAddressof(lvalue);
	}

	private SemaExpr visit(AstExprAlignof alignof, Maybe<SemaType> deduce)
	{
		return new SemaExprAlignof(TypeValidator.validate(alignof.type, scope, context, validateDecl));
	}

	private SemaExpr visit(AstExprArrayAccess arrayAccess, Maybe<SemaType> deduce)
	{
		SemaExpr value = validate(arrayAccess.value, scope, context, validateDecl, Maybe.none());
		SemaExpr index = validate(arrayAccess.index, scope, context, validateDecl, Maybe.some(SemaTypeBuiltin.INT));
		SemaType indexType = index.type();

		if(TypeHelper.isBuiltinType(indexType, BuiltinType.INT))
			throw new RuntimeException("array access requires index of type 'int'");

		if(TypeHelper.isArrayType(value.type()))
			throw new RuntimeException("array access requires expression yielding array type");

		if(value instanceof SemaExprLValueExpr)
			return new SemaExprArrayAccessLValue((SemaExprLValueExpr)value, index);

		return new SemaExprArrayAccessRValue(value, index);
	}

	private SemaExpr visit(AstExprAssign assign, Maybe<SemaType> deduce)
	{
		SemaExpr left = validate(assign.left, scope, context, validateDecl, Maybe.none());

		if(TypeHelper.isVoidType(left.type()))
			throw new RuntimeException("value expected on left side of assignment, got expression yielding 'void'");

		SemaType leftType = left.type();
		SemaType leftTypeDecayed = TypeHelper.decay(leftType);

		SemaExpr right = validate(assign.right, scope, context, validateDecl, Maybe.some(leftTypeDecayed));

		if(TypeHelper.isVoidType(right.type()))
			throw new RuntimeException("value expected on right side of assignment, got expression yielding 'void'");

		if(TypeHelper.isConst(leftType) || !(left instanceof SemaExprLValueExpr))
			throw new RuntimeException("non-const lvalue required on left side of assignment");

		if(leftType instanceof SemaTypeFunction)
			throw new RuntimeException("cannot assign to value of function type");

		SemaType rightTypeDecayed = TypeHelper.decay(right.type());

		if(!SemaType.same(leftTypeDecayed, rightTypeDecayed))
		{
			String fmt = "compatible types expected in assignment, got '%s' and '%s'";
			throw new RuntimeException(String.format(fmt, TypeString.of(leftTypeDecayed), TypeString.of(rightTypeDecayed)));
		}

		SemaExprLValueExpr leftAsLvalue = (SemaExprLValueExpr)left;
		leftAsLvalue.useAsLValue(true);
		return new SemaExprAssign(leftAsLvalue, right);
	}

	private SemaExpr visit(AstExprBuiltinCall builtinCall, Maybe<SemaType> deduce)
	{
		Maybe<BuiltinFunc> maybeFunc = context.findBuiltin(builtinCall.name);

		if(maybeFunc.isNone())
			throw new RuntimeException(String.format("builtin '%s' not found", builtinCall.name));

		List<SemaExpr> args = new ArrayList<>();
		List<SemaType> types = new ArrayList<>();

		for(int i = 0; i != builtinCall.args.size(); ++i)
		{
			SemaExpr semaExpr = validate(builtinCall.args.get(i), scope, context, validateDecl, Maybe.none());
			SemaType type = semaExpr.type();

			if(TypeHelper.isVoidType(type))
			{
				String fmt = "expression passed to builtin '%s' as argument %s yields 'void'";
				throw new RuntimeException(String.format(fmt, builtinCall.name, i + 1));
			}

			args.add(semaExpr);
			types.add(type);
		}

		BuiltinFunc func = maybeFunc.unwrap();
		SemaType ret = func.validateCall(types);
		return new SemaExprBuiltinCall(func, args, ret);
	}

	private SemaExpr visit(AstExprConst const_, Maybe<SemaType> deduce)
	{
		SemaExpr expr = validate(const_.expr, scope, context, validateDecl, deduce);

		if(TypeHelper.isVoidType(expr.type()))
			throw new RuntimeException("expression passed to const symbol yields 'void'");

		if(TypeHelper.isFunctionType(expr.type()))
			throw new RuntimeException("const symbol applied to value of function type");

		if(expr instanceof SemaExprLValueExpr)
			return new SemaExprConstLValue((SemaExprLValueExpr)expr);

		return new SemaExprConstRValue(expr);
	}

	private SemaExpr visit(AstExprDeref deref, Maybe<SemaType> deduce)
	{
		SemaExpr expr = validate(deref.expr, scope, context, validateDecl, deduce.map(SemaTypePointer::new));

		if(!TypeHelper.isPointerType(expr.type()))
		{
			String fmt = "expected expression of pointer type in 'deref', got '%s'";
			throw new RuntimeException(String.format(fmt, TypeString.of(expr.type())));
		}

		return new SemaExprDeref(expr);
	}

	private Maybe<List<SemaExpr>> match(SemaDeclFunction function, List<AstExpr> args)
	{
		List<SemaExpr> result = new ArrayList<>();

		for(int i = 0; i != function.params.size(); ++i)
		{
			SemaType paramType = function.params.get(i).type;
			SemaType paramTypeDecayed = TypeHelper.decay(paramType);

			SemaExpr arg;

			try
			{
				arg = ExprValidator.validate(args.get(i), scope, context, validateDecl, Maybe.some(paramTypeDecayed));
			}

			catch(RuntimeException e)
			{
				return Maybe.none();
			}

			if(TypeHelper.isVoidType(arg.type()))
			{
				String fmt = "expression passed as argument %s to function '%s' yields 'void'";
				throw new RuntimeException(String.format(fmt, i + 1, function.name()));
			}

			SemaType argTypeDecayed = TypeHelper.decay(arg.type());

			if(!SemaType.same(paramTypeDecayed, argTypeDecayed))
				return Maybe.none();

			result.add(arg);
		}

		return Maybe.some(result);
	}

	private SemaExpr resolveOverloadedCall(List<SemaDeclFunction> set, String name, List<AstExpr> args, Maybe<Boolean> inline)
	{
		IdentityHashMap<SemaDeclFunction, List<SemaExpr>> candidates = new IdentityHashMap<>();

		for(SemaDeclFunction overload : set)
		{
			if(overload.params.size() != args.size())
				continue;

			Maybe<List<SemaExpr>> semaArgs = match(overload, args);

			if(semaArgs.isSome())
				candidates.put(overload, semaArgs.unwrap());
		}

		if(candidates.isEmpty())
		{
			String fmt = "no matching function for call to '%s' out of %s overloads";
			throw new RuntimeException(String.format(fmt, name, set.size()));
		}

		if(candidates.size() > 1)
			throw new RuntimeException(String.format("ambiguous call to function '%s'", name));

		Map.Entry<SemaDeclFunction, List<SemaExpr>> first = candidates.entrySet().iterator().next();
		return new SemaExprDirectFunctionCall(first.getKey(), first.getValue(), inline);
	}

	private SemaExpr visit(AstExprFunctionCall call, Maybe<SemaType> deduce)
	{
		SemaExpr expr = validate(call.expr, scope, context, validateDecl, Maybe.none());

		if(expr instanceof SemaExprNamedImportedOverloadSet)
		{
			SemaDeclOverloadSet set = ((SemaExprNamedImportedOverloadSet)expr).set.set;

			List<SemaDeclFunction> candidates = new ArrayList<>();

			for(SemaDeclFunction function : set.overloads)
				if(function.exported)
					candidates.add(function);

			return resolveOverloadedCall(candidates, set.name(), call.args, call.inline);
		}

		if(expr instanceof SemaExprNamedOverloadSet)
		{
			SemaDeclOverloadSet set = ((SemaExprNamedOverloadSet)expr).set;
			return resolveOverloadedCall(set.overloads, set.name(), call.args, call.inline);
		}

		if(!TypeHelper.isFunctionType(expr.type()))
			throw new RuntimeException("left side of function call does not yield a function type");

		SemaTypeFunction ftype = (SemaTypeFunction)expr.type();
		List<SemaExpr> args = new ArrayList<>();

		for(int i = 0; i != call.args.size(); ++i)
		{
			AstExpr arg = call.args.get(i);
			SemaType type = ftype.params.get(i);
			args.add(validate(call.args.get(i), scope, context, validateDecl, Maybe.some(type)));
		}

		checkArguments(ftype.params, args);

		return new SemaExprIndirectFunctionCall(expr, args);
	}

	private SemaExpr visit(AstExprLitArray lit, Maybe<SemaType> deduce)
	{
		Maybe<BigInteger> length = lit.length;
		Maybe<SemaType> maybeType = lit.type.map(type -> TypeValidator.validate(type, scope, context, validateDecl));

		if(deduce.isSome() && deduce.unwrap() instanceof SemaTypeArray)
		{
			SemaTypeArray array = (SemaTypeArray)deduce.unwrap();

			if(length.isNone())
				length = Maybe.some(array.length);

			if(maybeType.isNone())
				maybeType = Maybe.some(array.type);
		}

		if(length.isNone())
			length = Maybe.some(BigInteger.valueOf(lit.values.size()));

		if(maybeType.isNone())
			throw new RuntimeException("element type of array literal could not be deduced");

		SemaType type = maybeType.unwrap();
		SemaType typeDecayed = TypeHelper.decay(type);

		List<SemaExpr> values = new ArrayList<>();

		for(int i = 0; i != lit.values.size(); ++i)
		{
			SemaExpr semaExpr = validate(lit.values.get(i), scope, context, validateDecl, maybeType);
			SemaType elemTypeDecayed = TypeHelper.decay(semaExpr.type());

			if(!SemaType.same(elemTypeDecayed, typeDecayed))
			{
				String fmt = "element in array literal at index %s has type '%s', expected '%s'";
				throw new RuntimeException(String.format(fmt, i, TypeString.of(elemTypeDecayed), TypeString.of(typeDecayed)));
			}

			values.add(semaExpr);
		}

		if(BigInteger.valueOf(values.size()).compareTo(length.unwrap()) != 0)
		{
			String fmt = "invalid number of elements in array literal: got %s, expected %s";
			throw new RuntimeException(String.format(fmt, values.size(), length.unwrap()));
		}

		return new SemaExprLitArray(length.unwrap(), maybeType.unwrap(), values);
	}

	private SemaExpr visit(AstExprLitBool lit, Maybe<SemaType> deduce)
	{
		return new SemaExprLitBool(lit.value);
	}

	private void errorLiteralTypeDeduction()
	{
		throw new RuntimeException("type of literal could not be deduced");
	}

	private BuiltinType deduceLiteralType(Maybe<SemaType> maybeType, boolean floatingPoint)
	{
		if(maybeType.isNone())
			errorLiteralTypeDeduction();

		SemaType type = maybeType.unwrap();

		if(type instanceof SemaTypeConst)
			type = ((SemaTypeConst)type).type;

		if(!(type instanceof SemaTypeBuiltin))
			errorLiteralTypeDeduction();

		SemaTypeBuiltin builtin = (SemaTypeBuiltin)type;

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

	private SemaExpr visit(AstExprLitFloat lit, Maybe<SemaType> deduce)
	{
		if(lit.type.isNone())
			lit.type = Maybe.some(deduceLiteralType(deduce, true));

		if(!Limits.inRange(lit.value, lit.type.unwrap()))
			throw new RuntimeException(String.format("floating point literal value is out of range: %s", lit.value));

		return new SemaExprLitFloat(lit.value, lit.type.unwrap());
	}

	private SemaExpr visit(AstExprLitInt lit, Maybe<SemaType> deduce)
	{
		if(lit.type.isNone())
			lit.type = Maybe.some(deduceLiteralType(deduce, false));

		if(!Limits.inRange(lit.value, lit.type.unwrap(), context))
			throw new RuntimeException(String.format("integer literal value is out of range: %s", lit.value));

		return new SemaExprLitInt(lit.value, lit.type.unwrap());
	}

	private SemaExpr visit(AstExprLitNull lit, Maybe<SemaType> deduce)
	{
		return new SemaExprLitNull();
	}

	private SemaExpr visit(AstExprLitString lit, Maybe<SemaType> deduce)
	{
		return new SemaExprLitString(lit.value);
	}

	private SemaExpr namedDeclExpr(SemaDecl decl, boolean globalAccess)
	{
		validateDecl.accept(decl);

		if(decl instanceof SemaDeclGlobal)
		{
			if(!globalAccess)
				throw new RuntimeException("reference to global requires 'global' keyword");

			return new SemaExprNamedGlobal((SemaDeclGlobal)decl);
		}

		if(globalAccess)
			throw new RuntimeException("'global' keyword used on reference to symbol that is not a global");

		if(decl instanceof SemaDeclOverloadSet)
			return new SemaExprNamedOverloadSet((SemaDeclOverloadSet)decl);

		if(decl instanceof SemaDeclImportedOverloadSet)
			return new SemaExprNamedImportedOverloadSet((SemaDeclImportedOverloadSet)decl);

		throw new AssertionError("unreachable");
	}

	private void errorNoSuchField(SemaType type, String fieldName)
	{
		throw new RuntimeException(String.format("type '%s' has no field named '%s'", TypeString.of(type), fieldName));
	}

	private SemaExpr visit(AstExprMemberAccess memberAccess, Maybe<SemaType> deduce)
	{
		SemaExpr expr = validate(memberAccess.expr, scope, context, validateDecl, Maybe.none());

		if(expr instanceof SemaExprNamedRenamedImport)
		{
			SemaDeclRenamedImport import_ = ((SemaExprNamedRenamedImport)expr).import_;
			SemaDecl decl = import_.decls.get(memberAccess.name);

			if(decl == null)
				throw new RuntimeException(String.format("reference to unknown symbol '%s.%s'", import_.module.path(), memberAccess.name));

			return namedDeclExpr(decl, memberAccess.global);
		}

		if(TypeHelper.isVoidType(expr.type()))
			throw new RuntimeException("left side of member access yields 'void'");

		SemaType type = expr.type();

		if(!(type instanceof SemaTypeUserDefined))
			errorNoSuchField(type, memberAccess.name);

		SemaDeclData data = ((SemaTypeUserDefined)type).data;
		Maybe<SemaDeclData.Field> field = data.findField(memberAccess.name);

		if(field.isNone())
			errorNoSuchField(type, memberAccess.name);

		if(expr instanceof SemaExprLValueExpr)
			return new SemaExprFieldAccessLValue((SemaExprLValueExpr)expr, field.unwrap());

		return new SemaExprFieldAccessRValue(expr, field.unwrap());
	}

	private SemaExpr visit(AstExprNamedGlobal namedGlobal, Maybe<SemaType> deduce)
	{
		List<SemaSymbol> candidates = scope.find(namedGlobal.name);

		if(candidates.isEmpty())
			throw new RuntimeException(String.format("reference to unknown symbol '%s'", namedGlobal.name));

		if(candidates.size() > 1)
			throw new RuntimeException(String.format("ambiguos reference to symbol '%s'", namedGlobal.name));

		SemaSymbol symbol = candidates.get(0);

		if(!(symbol instanceof SemaDeclGlobal))
			throw new RuntimeException(String.format("symbol '%s' does not refer to a global", namedGlobal.name));

		return namedDeclExpr((SemaDecl)symbol, true);
	}

	private SemaExpr visit(AstExprNamedSymbol namedSymbol, Maybe<SemaType> deduce)
	{
		List<SemaSymbol> candidates = scope.find(namedSymbol.name);

		if(candidates.isEmpty())
			throw new RuntimeException(String.format("reference to unknown symbol '%s'", namedSymbol.name));

		if(candidates.size() > 1)
			throw new RuntimeException(String.format("ambiguous reference to symbol '%s'", namedSymbol.name));

		SemaSymbol symbol = candidates.get(0);

		if(symbol instanceof SemaDeclRenamedImport)
			return new SemaExprNamedRenamedImport((SemaDeclRenamedImport)symbol);

		if(symbol instanceof SemaDecl)
			return namedDeclExpr((SemaDecl)symbol, false);

		if(symbol instanceof SemaDeclDefinedFunction.Local)
			return new SemaExprNamedLocal((SemaDeclDefinedFunction.Local)symbol);

		if(symbol instanceof SemaDeclFunction.Param)
			return new SemaExprNamedParam((SemaDeclFunction.Param)symbol);

		throw new RuntimeException(String.format("symbol '%s' does not refer to a value", namedSymbol.name));
	}

	private SemaExpr visit(AstExprOffsetof offsetof, Maybe<SemaType> deduce)
	{
		List<SemaSymbol> candidates = scope.find(offsetof.type);

		if(candidates.isEmpty())
			throw new RuntimeException(String.format("use of unknown type '%s'", offsetof.type));

		if(candidates.size() > 1)
			throw new RuntimeException(String.format("ambiguous reference to symbol '%s'", offsetof.type));

		SemaSymbol symbol = candidates.get(0);

		if(!(symbol instanceof SemaDeclData))
			throw new RuntimeException(String.format("symbol '%s' does not refer to a type", offsetof.type));

		Maybe<SemaDeclData.Field> field = ((SemaDeclData)symbol).findField(offsetof.field);

		if(field.isNone())
			errorNoSuchField(new SemaTypeUserDefined((SemaDeclData)symbol), offsetof.field);

		return new SemaExprOffsetof(field.unwrap());
	}

	private SemaExpr handleOperatorCall(String op, Kind kind, List<AstExpr> args, Maybe<SemaType> deduce)
	{
		List<SemaSymbol> candidates = scope.find(Operator.implName(op, kind));

		if(candidates.isEmpty())
			throw new RuntimeException(String.format("no implementation of operator '%s' found", op));

		if(candidates.size() > 1)
			throw new RuntimeException("nyi");

		SemaDeclOverloadSet set = (SemaDeclOverloadSet)candidates.get(0);
		return resolveOverloadedCall(set.overloads, set.name(), args, Maybe.none());
	}

	private SemaExpr visit(AstExprOpInfix op, Maybe<SemaType> deduce)
	{
		return handleOperatorCall(op.decl.operator.symbol, Kind.INFIX, Arrays.asList(op.left, op.right), deduce);
	}

	private SemaExpr visit(AstExprOpPrefix op, Maybe<SemaType> deduce)
	{
		return handleOperatorCall(op.decl.operator.symbol, Kind.PREFIX, Collections.singletonList(op.expr), deduce);
	}

	private SemaExpr visit(AstExprOpPostfix op, Maybe<SemaType> deduce)
	{
		return handleOperatorCall(op.decl.operator.symbol, Kind.POSTFIX, Collections.singletonList(op.expr), deduce);
	}

	private SemaExpr visit(AstExprSizeof sizeof, Maybe<SemaType> deduce)
	{
		return new SemaExprSizeof(TypeValidator.validate(sizeof.type, scope, context, validateDecl));
	}
}
