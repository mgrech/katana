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

package io.katana.compiler.analysis;

import io.katana.compiler.BuiltinType;
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
		var validator = new ExprValidator(scope, context, validateDecl);
		var result = (SemaExpr)expr.accept(validator, deduce);

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

	private SemaExpr visit(AstExprAddressof addressof, Maybe<SemaType> deduce)
	{
		var expr = validate(addressof.expr, scope, context, validateDecl, Maybe.none());

		if(expr instanceof SemaExprNamedOverloadSet)
		{
			var set = ((SemaExprNamedOverloadSet)expr).set;

			if(set.overloads.size() > 1)
				throw new CompileException(String.format("cannot take address of function '%s': function is overloaded", set.qualifiedName()));

			expr = new SemaExprNamedFunc(set.overloads.get(0));
		}

		if(expr.kind() != ExprKind.LVALUE)
			throw new CompileException("address-of operator ('prefix &') requires lvalue operand");

		return new SemaExprAddressof(expr);
	}

	private SemaExpr visit(AstExprAlignof alignof, Maybe<SemaType> deduce)
	{
		return new SemaExprAlignof(TypeValidator.validate(alignof.type, scope, context, validateDecl));
	}

	private SemaExpr visit(AstExprIndexAccess indexAccess, Maybe<SemaType> deduce)
	{
		var value = validate(indexAccess.value, scope, context, validateDecl, Maybe.none());
		var index = validate(indexAccess.index, scope, context, validateDecl, Maybe.some(SemaTypeBuiltin.INT));
		var indexType = index.type();

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

	private SemaExpr visit(AstExprAssign assign, Maybe<SemaType> deduce)
	{
		var left = validate(assign.left, scope, context, validateDecl, Maybe.none());

		var leftType = left.type();
		var leftTypeNoConst = Types.removeConst(leftType);

		var right = validate(assign.right, scope, context, validateDecl, Maybe.some(leftTypeNoConst));

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

	private SemaExpr visit(AstExprBuiltinCall builtinCall, Maybe<SemaType> deduce)
	{
		var maybeFunc = context.findBuiltin(builtinCall.name);

		if(maybeFunc.isNone())
			throw new CompileException(String.format("builtin '%s' not found", builtinCall.name));

		var args = new ArrayList<SemaExpr>();

		for(var i = 0; i != builtinCall.args.size(); ++i)
		{
			var semaExpr = validate(builtinCall.args.get(i), scope, context, validateDecl, Maybe.none());
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
		return builtin.validateCall(args);
	}

	private SemaExpr visit(AstExprConst const_, Maybe<SemaType> deduce)
	{
		var expr = validate(const_.expr, scope, context, validateDecl, deduce);

		if(Types.isFunction(expr.type()))
			throw new CompileException("const symbol applied to value of function type");

		return new SemaExprConst(expr);
	}

	private SemaExpr visit(AstExprDeref deref, Maybe<SemaType> deduce)
	{
		var expr = validate(deref.expr, scope, context, validateDecl, deduce.map(SemaTypeNonNullablePointer::new));

		if(!Types.isPointer(expr.type()))
		{
			var fmt = "expected expression of pointer type in dereference operator ('prefix *'), got '%s'";
			throw new CompileException(String.format(fmt, TypeString.of(expr.type())));
		}

		if(expr.kind() == ExprKind.LVALUE)
			expr = new SemaExprImplicitConversionLValueToRValue(expr);

		return new SemaExprDeref(expr);
	}

	private boolean match(SemaDeclFunction function, List<AstExpr> args, List<Maybe<SemaExpr>> result)
	{
		var failed = false;

		for(var i = 0; i != function.params.size(); ++i)
		{
			var paramType = function.params.get(i).type;
			var paramTypeNoConst = Types.removeConst(paramType);

			try
			{
				var arg = ExprValidator.validate(args.get(i), scope, context, validateDecl, Maybe.some(paramTypeNoConst));
				var argTypeNoConst = Types.removeConst(arg.type());

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

			for(var i = 1; i != overload.params.size(); ++i)
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

			for(var i = 1; i != args.size(); ++i)
			{
				builder.append(", ");
				appendArg(builder, args.get(i));
			}
		}

		builder.append(')');
	}

	private static void appendDeducedOverloads(StringBuilder builder, IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>> overloads)
	{
		for(var entry : overloads.entrySet())
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

			for(var overload : invalidNumArgs)
			{
				builder.append("\t\t");
				appendSignature(builder, overload);
				builder.append('\n');
			}
		}
	}

	private SemaExpr resolveOverloadedCall(List<SemaDeclFunction> set, String name, List<AstExpr> args, Inlining inline)
	{
		var candidates = new IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>>();
		var failed = new IdentityHashMap<SemaDeclFunction, List<Maybe<SemaExpr>>>();
		var other = Collections.newSetFromMap(new IdentityHashMap<SemaDeclFunction, Boolean>());

		for(var overload : set)
		{
			if(overload.params.size() != args.size())
			{
				other.add(overload);
				continue;
			}

			var semaArgs = new ArrayList<Maybe<SemaExpr>>();

			if(match(overload, args, semaArgs))
				candidates.put(overload, semaArgs);
			else
				failed.put(overload, semaArgs);
		}

		if(candidates.isEmpty())
		{
			var builder = new StringBuilder();
			builder.append(String.format("no matching function for call to '%s' out of %s overloads:\n", name, set.size()));
			appendOverloadInfo(builder, failed, other);
			throw new CompileException(builder.toString());
		}

		if(candidates.size() > 1)
		{
			var builder = new StringBuilder();
			builder.append(String.format("ambiguous call to function '%s', %s candidates:\n", name, candidates.size()));

			builder.append("\tmatching succeeded for the following overloads:\n");
			appendDeducedOverloads(builder, candidates);

			appendOverloadInfo(builder, failed, other);

			throw new CompileException(builder.toString());
		}

		var first = candidates.entrySet().iterator().next();

		var semaArgs = new ArrayList<SemaExpr>();

		for(var maybeArg : first.getValue())
		{
			var arg = maybeArg.unwrap();

			if(arg.kind() == ExprKind.LVALUE)
				arg = new SemaExprImplicitConversionLValueToRValue(arg);

			semaArgs.add(arg);
		}

		return new SemaExprDirectFunctionCall(first.getKey(), semaArgs, inline);
	}

	private SemaExpr visit(AstExprFunctionCall call, Maybe<SemaType> deduce)
	{
		var expr = validate(call.expr, scope, context, validateDecl, Maybe.none());

		if(expr instanceof SemaExprNamedImportedOverloadSet)
		{
			var set = ((SemaExprNamedImportedOverloadSet)expr).set.set;

			var candidates = new ArrayList<SemaDeclFunction>();

			for(var function : set.overloads)
				if(function.exported)
					candidates.add(function);

			return resolveOverloadedCall(candidates, set.name(), call.args, call.inline);
		}

		if(expr instanceof SemaExprNamedOverloadSet)
		{
			var set = ((SemaExprNamedOverloadSet)expr).set;
			return resolveOverloadedCall(set.overloads, set.name(), call.args, call.inline);
		}

		if(!Types.isFunction(expr.type()))
			throw new CompileException("left side of function call does not yield a function type");

		var ftype = (SemaTypeFunction)expr.type();
		var args = new ArrayList<SemaExpr>();

		for(var i = 0; i != call.args.size(); ++i)
		{
			var arg = call.args.get(i);
			var type = ftype.params.get(i);

			var semaArg = validate(call.args.get(i), scope, context, validateDecl, Maybe.some(type));

			if(semaArg.kind() == ExprKind.LVALUE)
				semaArg = new SemaExprImplicitConversionLValueToRValue(semaArg);

			args.add(semaArg);
		}

		checkArguments(ftype.params, args);

		return new SemaExprIndirectFunctionCall(expr, args);
	}

	private SemaExpr visit(AstExprLitArray lit, Maybe<SemaType> deduce)
	{
		var maybeType = lit.type.map(type -> TypeValidator.validate(type, scope, context, validateDecl));

		if(deduce.isSome() && deduce.unwrap() instanceof SemaTypeArray)
		{
			var array = (SemaTypeArray)deduce.unwrap();

			if(maybeType.isNone())
				maybeType = Maybe.some(array.type);
		}

		if(maybeType.isNone())
			throw new CompileException("element type of array literal could not be deduced");

		var type = maybeType.unwrap();
		var typeNoConst = Types.removeConst(type);

		var values = new ArrayList<SemaExpr>();

		for(var i = 0; i != lit.values.size(); ++i)
		{
			var semaExpr = validate(lit.values.get(i), scope, context, validateDecl, maybeType);
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

		var type = Types.removeConst(maybeType.unwrap());

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

	private SemaExpr visit(AstExprLitFloat lit, Maybe<SemaType> deduce)
	{
		var type = lit.type;

		if(type.isNone())
			type = Maybe.some(deduceLiteralType(deduce, true));

		if(lit.value == null)
			return new SemaExprLitFloat(null, type.unwrap());

		if(!Limits.inRange(lit.value, type.unwrap()))
			throw new CompileException(String.format("floating point literal value is out of range: %s", lit.value));

		return new SemaExprLitFloat(lit.value, type.unwrap());
	}

	private SemaExpr visit(AstExprLitInt lit, Maybe<SemaType> deduce)
	{
		var type = lit.type;

		if(type.isNone())
			type = Maybe.some(deduceLiteralType(deduce, false));

		if(lit.value == null)
			return new SemaExprLitInt(null, type.unwrap());

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
		var expr = validate(memberAccess.expr, scope, context, validateDecl, Maybe.none());

		if(expr instanceof SemaExprNamedRenamedImport)
		{
			var import_ = ((SemaExprNamedRenamedImport)expr).import_;
			var decl = import_.decls.get(memberAccess.name);

			if(decl == null)
				throw new CompileException(String.format("reference to unknown symbol '%s.%s'", import_.module.path(), memberAccess.name));

			return namedDeclExpr(decl, memberAccess.global);
		}

		var type = expr.type();

		// auto derefs
		while(Types.isPointer(type))
		{
			if(expr.kind() == ExprKind.LVALUE)
				expr = new SemaExprImplicitConversionLValueToRValue(expr);
			
			expr = new SemaExprDeref(expr);
			type = expr.type();
		}

		var typeNoConst = Types.removeConst(type);

		if(typeNoConst instanceof SemaTypeSlice)
		{
			switch(memberAccess.name)
			{
			case "pointer": return new SemaExprSliceGetPointer(expr);
			case "length":  return new SemaExprSliceGetLength(expr);
			case "sliceof": return expr;
			default: errorNoSuchField(type, memberAccess.name);
			}
		}

		if(typeNoConst instanceof SemaTypeArray)
		{
			switch(memberAccess.name)
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

			default: errorNoSuchField(type, memberAccess.name);
			}
		}

		if(!(typeNoConst instanceof SemaTypeStruct))
			errorNoSuchField(type, memberAccess.name);

		var struct = ((SemaTypeStruct)typeNoConst).decl;
		var field = struct.findField(memberAccess.name);

		if(field.isNone())
			errorNoSuchField(type, memberAccess.name);

		return new SemaExprFieldAccess(expr, field.unwrap(), Types.isConst(type));
	}

	private SemaExpr visit(AstExprNamedGlobal namedGlobal, Maybe<SemaType> deduce)
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

	private SemaExpr visit(AstExprNamedSymbol namedSymbol, Maybe<SemaType> deduce)
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

		if(symbol instanceof SemaDeclDefinedFunction.Variable)
			return new SemaExprNamedVar((SemaDeclDefinedFunction.Variable)symbol);

		if(symbol instanceof SemaDeclFunction.Param)
			return new SemaExprNamedParam((SemaDeclFunction.Param)symbol);

		throw new CompileException(String.format("symbol '%s' does not refer to a value", namedSymbol.name));
	}

	private SemaExpr validateCast(AstType type, AstExpr expr, SemaExprCast.Kind kind)
	{
		var targetType = TypeValidator.validate(type, scope, context, validateDecl);
		var semaExpr = validate(expr, scope, context, validateDecl, Maybe.none());
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

	private SemaExpr visit(AstExprNarrowCast cast, Maybe<SemaType> deduce)
	{
		return validateCast(cast.type, cast.expr, SemaExprCast.Kind.NARROW_CAST);
	}

	private SemaExpr visit(AstExprOffsetof offsetof, Maybe<SemaType> deduce)
	{
		var candidates = scope.find(offsetof.type);

		if(candidates.isEmpty())
			throw new CompileException(String.format("use of unknown type '%s'", offsetof.type));

		if(candidates.size() > 1)
			throw new CompileException(String.format("ambiguous reference to symbol '%s'", offsetof.type));

		var symbol = candidates.get(0);

		if(!(symbol instanceof SemaDeclStruct))
			throw new CompileException(String.format("symbol '%s' does not refer to a type", offsetof.type));

		var field = ((SemaDeclStruct)symbol).findField(offsetof.field);

		if(field.isNone())
			errorNoSuchField(new SemaTypeStruct((SemaDeclStruct)symbol), offsetof.field);

		return new SemaExprOffsetof(field.unwrap());
	}

	private SemaExpr handleOperatorCall(String op, Kind kind, List<AstExpr> args, Maybe<SemaType> deduce)
	{
		var candidates = scope.find(Operator.implName(op, kind));

		if(candidates.isEmpty())
			throw new CompileException(String.format("no implementation of operator '%s' found", op));

		if(candidates.size() > 1)
			throw new CompileException("nyi");

		var symbol = candidates.get(0);
		var set = symbol instanceof SemaDeclOverloadSet
		          ? (SemaDeclOverloadSet)symbol
		          : ((SemaDeclImportedOverloadSet)symbol).set;

		var overloads = set.overloads.stream()
		                             .filter(o -> o.exported)
		                             .collect(Collectors.toList());

		return resolveOverloadedCall(overloads, set.name(), args, Inlining.AUTO);
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
