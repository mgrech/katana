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

package katana.analysis;

import katana.BuiltinFunc;
import katana.BuiltinType;
import katana.Limits;
import katana.ast.expr.*;
import katana.ast.type.AstType;
import katana.backend.PlatformContext;
import katana.diag.CompileException;
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
import java.util.stream.Collectors;

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
		SemaExpr result = (SemaExpr)expr.accept(validator, deduce);

		if(deduce.isNone())
			return result;

		return ImplicitConversions.perform(result, deduce.unwrap());
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
			throw new CompileException(String.format(fmt, expected, actual));
		}

		Iterator<SemaType> it1 = params.iterator();
		Iterator<SemaExpr> it2 = args.iterator();

		for(int argCount = 1; it1.hasNext(); ++argCount)
		{
			SemaType paramType = it1.next();
			SemaType paramTypeNoConst = Types.removeConst(paramType);
			SemaType argType = it2.next().type();
			SemaType argTypeNoConst = Types.removeConst(argType);

			if(!Types.equal(paramTypeNoConst, argTypeNoConst))
			{
				String fmt = "type mismatch in argument %s: expected '%s', got '%s'";
				throw new CompileException(String.format(fmt, argCount, TypeString.of(paramTypeNoConst), TypeString.of(argTypeNoConst)));
			}
		}
	}

	private SemaExpr visit(AstExprAddressof addressof, Maybe<SemaType> deduce)
	{
		SemaExpr expr = validate(addressof.expr, scope, context, validateDecl, Maybe.none());

		if(expr instanceof SemaExprNamedOverloadSet)
		{
			SemaDeclOverloadSet set = ((SemaExprNamedOverloadSet)expr).set;

			if(set.overloads.size() > 1)
				throw new CompileException(String.format("cannot take address of function '%s': function is overloaded", set.qualifiedName()));

			expr = new SemaExprNamedFunc(set.overloads.get(0));
		}

		if(!(expr instanceof SemaExprLValueExpr))
			throw new CompileException("address-of operator ('prefix &') requires lvalue operand");

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

		if(!Types.isBuiltin(indexType, BuiltinType.INT))
			throw new CompileException(String.format("array access requires index of type 'int', got '%s'", TypeString.of(indexType)));

		if(!Types.isArray(value.type()))
			throw new CompileException(String.format("array access requires expression yielding array type, got '%s'", TypeString.of(value.type())));

		if(value instanceof SemaExprLValueExpr)
			return new SemaExprArrayAccessLValue((SemaExprLValueExpr)value, index);

		return new SemaExprArrayAccessRValue(value, index);
	}

	private SemaExpr visit(AstExprAssign assign, Maybe<SemaType> deduce)
	{
		SemaExpr left = validate(assign.left, scope, context, validateDecl, Maybe.none());

		SemaType leftType = left.type();
		SemaType leftTypeNoConst = Types.removeConst(leftType);

		SemaExpr right = validate(assign.right, scope, context, validateDecl, Maybe.some(leftTypeNoConst));

		if(Types.isConst(leftType) || !(left instanceof SemaExprLValueExpr))
			throw new CompileException("non-const lvalue required on left side of assignment");

		if(leftType instanceof SemaTypeFunction)
			throw new CompileException("cannot assign to value of function type");

		SemaType rightTypeNoConst = Types.removeConst(right.type());

		if(!Types.equal(leftTypeNoConst, rightTypeNoConst))
		{
			String fmt = "compatible types expected in assignment, got '%s' and '%s'";
			throw new CompileException(String.format(fmt, TypeString.of(leftTypeNoConst), TypeString.of(rightTypeNoConst)));
		}

		SemaExprLValueExpr leftAsLvalue = (SemaExprLValueExpr)left;
		leftAsLvalue.useAsLValue(true);
		return new SemaExprAssign(leftAsLvalue, right);
	}

	private SemaExpr visit(AstExprBuiltinCall builtinCall, Maybe<SemaType> deduce)
	{
		Maybe<BuiltinFunc> maybeFunc = context.findBuiltin(builtinCall.name);

		if(maybeFunc.isNone())
			throw new CompileException(String.format("builtin '%s' not found", builtinCall.name));

		List<SemaExpr> args = new ArrayList<>();
		List<SemaType> types = new ArrayList<>();

		for(int i = 0; i != builtinCall.args.size(); ++i)
		{
			SemaExpr semaExpr = validate(builtinCall.args.get(i), scope, context, validateDecl, Maybe.none());
			SemaType type = semaExpr.type();

			if(Types.isVoid(type))
			{
				String fmt = "expression passed to builtin '%s' as argument %s yields 'void'";
				throw new CompileException(String.format(fmt, builtinCall.name, i + 1));
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

		if(Types.isFunction(expr.type()))
			throw new CompileException("const symbol applied to value of function type");

		if(expr instanceof SemaExprLValueExpr)
			return new SemaExprConstLValue((SemaExprLValueExpr)expr);

		return new SemaExprConstRValue(expr);
	}

	private SemaExpr visit(AstExprDeref deref, Maybe<SemaType> deduce)
	{
		SemaExpr expr = validate(deref.expr, scope, context, validateDecl, deduce.map(SemaTypeNonNullablePointer::new));

		if(!Types.isPointer(expr.type()))
		{
			String fmt = "expected expression of pointer type in dereference operator ('prefix *'), got '%s'";
			throw new CompileException(String.format(fmt, TypeString.of(expr.type())));
		}

		return new SemaExprDeref(expr);
	}

	private boolean match(SemaDeclFunction function, List<AstExpr> args, List<Maybe<SemaExpr>> result)
	{
		boolean failed = false;

		for(int i = 0; i != function.params.size(); ++i)
		{
			SemaType paramType = function.params.get(i).type;
			SemaType paramTypeNoConst = Types.removeConst(paramType);

			try
			{
				SemaExpr arg = ExprValidator.validate(args.get(i), scope, context, validateDecl, Maybe.some(paramTypeNoConst));
				SemaType argTypeNoConst = Types.removeConst(arg.type());

				if(!Types.equal(paramTypeNoConst, argTypeNoConst))
					failed = true;

				result.add(Maybe.some(arg));
			}

			catch(CompileException e)
			{
				failed = true;
				result.add(Maybe.none());
			}
		}

		return !failed;
	}

	private static void appendSignature(StringBuilder builder, SemaDeclFunction overload)
	{
		builder.append(overload.name());
		builder.append('(');

		if(!overload.params.isEmpty())
		{
			builder.append(TypeString.of(overload.params.get(0).type));

			for(int i = 1; i != overload.params.size(); ++i)
			{
				builder.append(", ");
				builder.append(TypeString.of(overload.params.get(i).type));
			}
		}

		builder.append(')');
	}

	private static void appendArg(StringBuilder builder, Maybe<SemaExpr> arg)
	{
		if(arg.isSome())
			builder.append(TypeString.of(arg.unwrap().type()));
		else
			builder.append("<deduction-failed>");
	}

	private static void appendArgs(StringBuilder builder, List<Maybe<SemaExpr>> args)
	{
		builder.append('(');

		if(!args.isEmpty())
		{
			appendArg(builder, args.get(0));

			for(int i = 1; i != args.size(); ++i)
			{
				builder.append(", ");
				appendArg(builder, args.get(i));
			}
		}

		builder.append(')');
	}

	private static void appendDeducedOverloads(StringBuilder builder, IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>> overloads)
	{
		for(Map.Entry<SemaDeclFunction, List<Maybe<SemaExpr>>> entry : overloads.entrySet())
		{
			builder.append("\t\t");
			appendSignature(builder, entry.getKey());
			builder.append(" with arguments deduced to ");
			appendArgs(builder, entry.getValue());
			builder.append('\n');
		}
	}

	private static void appendOverloadInfo(StringBuilder builder, IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>> failed, Set<SemaDeclFunction> invalidNumArgs)
	{
		if(!failed.isEmpty())
		{
			builder.append("\tmatching failed for the following overloads:\n");
			appendDeducedOverloads(builder, failed);
		}

		if(!invalidNumArgs.isEmpty())
		{
			builder.append("\tthe following overloads have a non-matching number of parameters:\n");

			for(SemaDeclFunction overload : invalidNumArgs)
			{
				builder.append("\t\t");
				appendSignature(builder, overload);
				builder.append('\n');
			}
		}
	}

	private SemaExpr resolveOverloadedCall(List<SemaDeclFunction> set, String name, List<AstExpr> args, Maybe<Boolean> inline)
	{
		IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>> candidates = new IdentityHashMap<>();
		IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>> failed = new IdentityHashMap<>();
		Set<SemaDeclFunction> other = Collections.newSetFromMap(new IdentityHashMap<>());

		for(SemaDeclFunction overload : set)
		{
			if(overload.params.size() != args.size())
			{
				other.add(overload);
				continue;
			}

			List<Maybe<SemaExpr>> semaArgs = new ArrayList<>();

			if(match(overload, args, semaArgs))
				candidates.put(overload, semaArgs);
			else
				failed.put(overload, semaArgs);
		}

		if(candidates.isEmpty())
		{
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("no matching function for call to '%s' out of %s overloads:\n", name, set.size()));
			appendOverloadInfo(builder, failed, other);
			throw new CompileException(builder.toString());
		}

		if(candidates.size() > 1)
		{
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("ambiguous call to function '%s', %s candidates:\n", candidates.size(), name));

			builder.append("\tmatching succeeded for the following overloads:\n");
			appendDeducedOverloads(builder, candidates);

			appendOverloadInfo(builder, failed, other);

			throw new CompileException(builder.toString());
		}

		Map.Entry<SemaDeclFunction, List<Maybe<SemaExpr>>> first = candidates.entrySet().iterator().next();

		List<SemaExpr> semaArgs = new ArrayList<>();

		for(Maybe<SemaExpr> arg : first.getValue())
			semaArgs.add(arg.unwrap());

		return new SemaExprDirectFunctionCall(first.getKey(), semaArgs, inline);
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

		if(!Types.isFunction(expr.type()))
			throw new CompileException("left side of function call does not yield a function type");

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
			throw new CompileException("element type of array literal could not be deduced");

		SemaType type = maybeType.unwrap();
		SemaType typeNoConst = Types.removeConst(type);

		List<SemaExpr> values = new ArrayList<>();

		for(int i = 0; i != lit.values.size(); ++i)
		{
			SemaExpr semaExpr = validate(lit.values.get(i), scope, context, validateDecl, maybeType);
			SemaType elemTypeNoConst = Types.removeConst(semaExpr.type());

			if(!Types.equal(elemTypeNoConst, typeNoConst))
			{
				String fmt = "element in array literal at index %s has type '%s', expected '%s'";
				throw new CompileException(String.format(fmt, i, TypeString.of(elemTypeNoConst), TypeString.of(typeNoConst)));
			}

			values.add(semaExpr);
		}

		if(BigInteger.valueOf(values.size()).compareTo(length.unwrap()) != 0)
		{
			String fmt = "invalid number of elements in array literal: got %s, expected %s";
			throw new CompileException(String.format(fmt, values.size(), length.unwrap()));
		}

		return new SemaExprLitArray(length.unwrap(), maybeType.unwrap(), values);
	}

	private SemaExpr visit(AstExprLitBool lit, Maybe<SemaType> deduce)
	{
		return new SemaExprLitBool(lit.value);
	}

	private void errorLiteralTypeDeduction()
	{
		throw new CompileException("type of literal could not be deduced");
	}

	private BuiltinType deduceLiteralType(Maybe<SemaType> maybeType, boolean floatingPoint)
	{
		if(maybeType.isNone())
			errorLiteralTypeDeduction();

		SemaType type = Types.removeConst(maybeType.unwrap());

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
		Maybe<BuiltinType> type = lit.type;

		if(type.isNone())
			type = Maybe.some(deduceLiteralType(deduce, true));

		if(!Limits.inRange(lit.value, type.unwrap()))
			throw new CompileException(String.format("floating point literal value is out of range: %s", lit.value));

		return new SemaExprLitFloat(lit.value, type.unwrap());
	}

	private SemaExpr visit(AstExprLitInt lit, Maybe<SemaType> deduce)
	{
		Maybe<BuiltinType> type = lit.type;

		if(type.isNone())
			type = Maybe.some(deduceLiteralType(deduce, false));

		if(!Limits.inRange(lit.value, type.unwrap(), context))
			throw new CompileException(String.format("integer literal value is out of range: %s", lit.value));

		return new SemaExprLitInt(lit.value, type.unwrap());
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

	private SemaExpr visit(AstExprMemberAccess memberAccess, Maybe<SemaType> deduce)
	{
		SemaExpr expr = validate(memberAccess.expr, scope, context, validateDecl, Maybe.none());

		if(expr instanceof SemaExprNamedRenamedImport)
		{
			SemaDeclRenamedImport import_ = ((SemaExprNamedRenamedImport)expr).import_;
			SemaDecl decl = import_.decls.get(memberAccess.name);

			if(decl == null)
				throw new CompileException(String.format("reference to unknown symbol '%s.%s'", import_.module.path(), memberAccess.name));

			return namedDeclExpr(decl, memberAccess.global);
		}

		SemaType type = expr.type();
		SemaType typeNoConst = Types.removeConst(type);

		if(!(typeNoConst instanceof SemaTypeStruct))
			errorNoSuchField(type, memberAccess.name);

		SemaDeclStruct struct = ((SemaTypeStruct)typeNoConst).decl;
		Maybe<SemaDeclStruct.Field> field = struct.findField(memberAccess.name);

		if(field.isNone())
			errorNoSuchField(type, memberAccess.name);

		if(expr instanceof SemaExprLValueExpr)
			return new SemaExprFieldAccessLValue((SemaExprLValueExpr)expr, field.unwrap(), Types.isConst(type));

		return new SemaExprFieldAccessRValue(expr, field.unwrap(), Types.isConst(type));
	}

	private SemaExpr visit(AstExprNamedGlobal namedGlobal, Maybe<SemaType> deduce)
	{
		List<SemaSymbol> candidates = scope.find(namedGlobal.name);

		if(candidates.isEmpty())
			throw new CompileException(String.format("reference to unknown symbol '%s'", namedGlobal.name));

		if(candidates.size() > 1)
			throw new CompileException(String.format("ambiguos reference to symbol '%s'", namedGlobal.name));

		SemaSymbol symbol = candidates.get(0);

		if(!(symbol instanceof SemaDeclGlobal))
			throw new CompileException(String.format("symbol '%s' does not refer to a global", namedGlobal.name));

		return namedDeclExpr((SemaDecl)symbol, true);
	}

	private SemaExpr visit(AstExprNamedSymbol namedSymbol, Maybe<SemaType> deduce)
	{
		List<SemaSymbol> candidates = scope.find(namedSymbol.name);

		if(candidates.isEmpty())
			throw new CompileException(String.format("reference to unknown symbol '%s'", namedSymbol.name));

		if(candidates.size() > 1)
			throw new CompileException(String.format("ambiguous reference to symbol '%s'", namedSymbol.name));

		SemaSymbol symbol = candidates.get(0);

		if(symbol instanceof SemaDeclRenamedImport)
			return new SemaExprNamedRenamedImport((SemaDeclRenamedImport)symbol);

		if(symbol instanceof SemaDecl)
			return namedDeclExpr((SemaDecl)symbol, false);

		if(symbol instanceof SemaDeclDefinedFunction.Local)
			return new SemaExprNamedLocal((SemaDeclDefinedFunction.Local)symbol);

		if(symbol instanceof SemaDeclFunction.Param)
			return new SemaExprNamedParam((SemaDeclFunction.Param)symbol);

		throw new CompileException(String.format("symbol '%s' does not refer to a value", namedSymbol.name));
	}

	private SemaExpr validateCast(AstType type, AstExpr expr, SemaExprCast.Kind kind)
	{
		SemaType targetType = TypeValidator.validate(type, scope, context, validateDecl);
		SemaExpr semaExpr = validate(expr, scope, context, validateDecl, Maybe.none());
		SemaType sourceType = semaExpr.type();

		if(!CastValidator.isValidCast(sourceType, targetType, kind, context))
		{
			String fmt = "%s from expression of type '%s' to type '%s' is not valid";
			throw new CompileException(String.format(fmt, kind.toString().toLowerCase(), TypeString.of(sourceType), TypeString.of(targetType)));
		}

		return new SemaExprCast(targetType, semaExpr, kind);
	}

	private SemaExpr visit(AstExprNarrowCast cast, Maybe<SemaType> deduce)
	{
		return validateCast(cast.type, cast.expr, SemaExprCast.Kind.NARROW_CAST);
	}

	private SemaExpr visit(AstExprOffsetof offsetof, Maybe<SemaType> deduce)
	{
		List<SemaSymbol> candidates = scope.find(offsetof.type);

		if(candidates.isEmpty())
			throw new CompileException(String.format("use of unknown type '%s'", offsetof.type));

		if(candidates.size() > 1)
			throw new CompileException(String.format("ambiguous reference to symbol '%s'", offsetof.type));

		SemaSymbol symbol = candidates.get(0);

		if(!(symbol instanceof SemaDeclStruct))
			throw new CompileException(String.format("symbol '%s' does not refer to a type", offsetof.type));

		Maybe<SemaDeclStruct.Field> field = ((SemaDeclStruct)symbol).findField(offsetof.field);

		if(field.isNone())
			errorNoSuchField(new SemaTypeStruct((SemaDeclStruct)symbol), offsetof.field);

		return new SemaExprOffsetof(field.unwrap());
	}

	private SemaExpr handleOperatorCall(String op, Kind kind, List<AstExpr> args, Maybe<SemaType> deduce)
	{
		List<SemaSymbol> candidates = scope.find(Operator.implName(op, kind));

		if(candidates.isEmpty())
			throw new CompileException(String.format("no implementation of operator '%s' found", op));

		if(candidates.size() > 1)
			throw new CompileException("nyi");

		SemaSymbol symbol = candidates.get(0);
		SemaDeclOverloadSet set = symbol instanceof SemaDeclOverloadSet
			? (SemaDeclOverloadSet)symbol
			: ((SemaDeclImportedOverloadSet)symbol).set;

		List<SemaDeclFunction> overloads = set.overloads.stream()
		                                                .filter(o -> o.exported)
		                                                .collect(Collectors.toList());

		return resolveOverloadedCall(overloads, set.name(), args, Maybe.none());
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

	private SemaExpr visit(AstExprParens parens, Maybe<SemaType> deduce)
	{
		return validate(parens.expr, scope, context, validateDecl, deduce);
	}

	private SemaExpr visit(AstExprPointerCast cast, Maybe<SemaType> deduce)
	{
		return validateCast(cast.type, cast.expr, SemaExprCast.Kind.POINTER_CAST);
	}

	private SemaExpr visit(AstExprSignCast cast, Maybe<SemaType> deduce)
	{
		return validateCast(cast.type, cast.expr, SemaExprCast.Kind.SIGN_CAST);
	}

	private SemaExpr visit(AstExprSizeof sizeof, Maybe<SemaType> deduce)
	{
		return new SemaExprSizeof(TypeValidator.validate(sizeof.type, scope, context, validateDecl));
	}

	private SemaExpr visit(AstExprWidenCast cast, Maybe<SemaType> deduce)
	{
		return validateCast(cast.type, cast.expr, SemaExprCast.Kind.WIDEN_CAST);
	}
}
