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

package katana.sema;

import katana.BuiltinType;
import katana.Limits;
import katana.ast.expr.NamedSymbol;
import katana.backend.PlatformContext;
import katana.sema.decl.*;
import katana.sema.decl.Function;
import katana.sema.expr.*;
import katana.sema.type.*;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ExprValidator implements IVisitor
{
	private Scope scope;
	private PlatformContext context;
	private Consumer<Decl> validateDecl;

	private ExprValidator(Scope scope, PlatformContext context, Consumer<Decl> validateDecl)
	{
		this.scope = scope;
		this.context = context;
		this.validateDecl = validateDecl;
	}

	public static Expr validate(katana.ast.expr.Expr expr, Scope scope, PlatformContext context, Consumer<Decl> validateDecl, Maybe<Type> deduce)
	{
		ExprValidator validator = new ExprValidator(scope, context, validateDecl);
		return (Expr)expr.accept(validator, deduce);
	}

	private void checkArguments(List<Type> params, List<Expr> args)
	{
		int expected = params.size();
		int actual = args.size();

		if(actual != expected)
		{
			String fmt = "invalid number of arguments: expected %s, %s given";
			throw new RuntimeException(String.format(fmt, expected, actual));
		}

		Iterator<Type> it1 = params.iterator();
		Iterator<Expr> it2 = args.iterator();
		int argCount = 1;

		while(it1.hasNext())
		{
			Type param = it1.next();
			Maybe<Type> arg = it2.next().type();

			if(arg.isNone())
				throw new RuntimeException("expression given in argument " + argCount + " results in no value");

			if(!Type.same(param, arg.unwrap()))
			{
				String fmt = "type mismatch in argument %s: expected '%s', got '%s'";
				throw new RuntimeException(String.format(fmt, argCount, param, arg.unwrap()));
			}

			++argCount;
		}
	}

	private Expr visit(katana.ast.expr.Addressof addressof, Maybe<Type> deduce)
	{
		Expr expr = validate(addressof.expr, scope, context, validateDecl, Maybe.none());

		if(!(expr instanceof LValueExpr))
			throw new RuntimeException("addressof() requires lvalue operand");

		LValueExpr lvalue = (LValueExpr)expr;
		lvalue.useAsLValue(true);
		return new Addressof(lvalue);
	}

	private Expr visit(katana.ast.expr.Alignof alignof, Maybe<Type> deduce)
	{
		return new Alignof(TypeValidator.validate(alignof.type, scope, context, validateDecl));
	}

	private Expr visit(katana.ast.expr.ArrayAccess arrayAccess, Maybe<Type> deduce)
	{
		Expr value = validate(arrayAccess.value, scope, context, validateDecl, Maybe.none());
		Expr index = validate(arrayAccess.index, scope, context, validateDecl, Maybe.some(Builtin.INT));
		Maybe<Type> indexType = index.type();

		if(indexType.isNone() || indexType.unwrap() != Builtin.INT)
			throw new RuntimeException("array access requires index of type int");

		if(value.type().isNone() || !(value.type().unwrap() instanceof Array))
			throw new RuntimeException("array access requires expression yielding array type");

		if(value instanceof LValueExpr)
			return new ArrayAccessLValue((LValueExpr)value, index);

		return new ArrayAccessRValue(value, index);
	}

	private Expr visit(katana.ast.expr.Assign assign, Maybe<Type> deduce)
	{
		Expr left = validate(assign.left, scope, context, validateDecl, Maybe.none());

		if(left.type().isNone())
			throw new RuntimeException("value expected on left side of assignment, got expression yielding void");

		Type leftType = left.type().unwrap();
		Type leftTypeStripped = TypeHelper.removeConst(leftType);

		Expr right = validate(assign.right, scope, context, validateDecl, Maybe.some(leftTypeStripped));

		if(right.type().isNone())
			throw new RuntimeException("value expected on right side of assignment, got expression yielding void");

		Type rightType = right.type().unwrap();

		if(TypeHelper.isConst(leftType) || !(left instanceof LValueExpr))
			throw new RuntimeException("non-const lvalue required on left side of assignment");

		if(leftType instanceof katana.sema.type.Function)
			throw new RuntimeException("cannot assign to value of function type");

		Type rightTypeStripped = TypeHelper.removeConst(rightType);

		if(!Type.same(leftTypeStripped, rightTypeStripped))
			throw new RuntimeException("same types expected in assignment");

		LValueExpr leftAsLvalue = (LValueExpr)left;
		leftAsLvalue.useAsLValue(true);
		return new Assign(leftAsLvalue, right);
	}

	private Expr visit(katana.ast.expr.BuiltinCall builtinCall, Maybe<Type> deduce)
	{
		Maybe<BuiltinFunc> maybeFunc = context.findBuiltin(builtinCall.name);

		if(maybeFunc.isNone())
			throw new RuntimeException(String.format("builtin %s not found", builtinCall.name));

		List<Expr> args = new ArrayList<>();
		List<Type> types = new ArrayList<>();

		for(int i = 0; i != builtinCall.args.size(); ++i)
		{
			Expr semaExpr = validate(builtinCall.args.get(i), scope, context, validateDecl, Maybe.none());
			Maybe<Type> type = semaExpr.type();

			if(type.isNone())
			{
				String fmt = "expression passed to builtin %s as argument %s yields no value";
				throw new RuntimeException(String.format(fmt, builtinCall.name, i + 1));
			}

			args.add(semaExpr);
			types.add(type.unwrap());
		}

		BuiltinFunc func = maybeFunc.unwrap();
		Maybe<Type> ret = func.validateCall(types);
		return new BuiltinCall(func, args, ret);
	}

	private Expr visit(katana.ast.expr.Const const_, Maybe<Type> deduce)
	{
		Expr expr = validate(const_.expr, scope, context, validateDecl, deduce);

		if(expr.type().isNone())
			throw new RuntimeException("expression passed to const operator yields void");

		Type type = expr.type().unwrap();

		if(type instanceof katana.sema.type.Function)
			throw new RuntimeException("const operator applied to value of function type");

		if(expr instanceof LValueExpr)
			return new ConstLValue((LValueExpr)expr);

		return new ConstRValue(expr);
	}

	private Expr visit(katana.ast.expr.Deref deref, Maybe<Type> deduce)
	{
		Expr expr = validate(deref.expr, scope, context, validateDecl, Maybe.some(Builtin.PTR));

		if(expr.type().isNone() || expr.type().unwrap() != Builtin.PTR)
			throw new RuntimeException("expression of type ptr expected in deref");

		return new Deref(TypeValidator.validate(deref.type, scope, context, validateDecl), expr);
	}

	private Expr visit(katana.ast.expr.FunctionCall call, Maybe<Type> deduce)
	{
		Expr expr = validate(call.expr, scope, context, validateDecl, Maybe.none());

		if(expr.type().isNone() || !(expr.type().unwrap() instanceof katana.sema.type.Function))
			throw new RuntimeException("expression does not result in function type");

		katana.sema.type.Function ftype = (katana.sema.type.Function)expr.type().unwrap();
		List<Expr> args = new ArrayList<>();

		for(int i = 0; i != call.args.size(); ++i)
		{
			katana.ast.expr.Expr arg = call.args.get(i);
			Type type = ftype.params.get(i);
			args.add(validate(call.args.get(i), scope, context, validateDecl, Maybe.some(type)));
		}

		checkArguments(ftype.params, args);

		if(expr instanceof NamedFunc)
			return new DirectFunctionCall(((NamedFunc)expr).func, args, call.inline);

		return new IndirectFunctionCall(expr, args);
	}

	private Expr visit(katana.ast.expr.LitArray lit, Maybe<Type> deduce)
	{
		Maybe<BigInteger> length = lit.length;
		Maybe<Type> maybeType = lit.type.map((t) -> TypeValidator.validate(t, scope, context, validateDecl));

		if(deduce.isSome() && deduce.unwrap() instanceof Array)
		{
			Array array = (Array)deduce.unwrap();

			if(length.isNone())
				length = Maybe.some(array.length);

			if(maybeType.isNone())
				maybeType = Maybe.some(array.type);
		}

		if(length.isNone())
			length = Maybe.some(BigInteger.valueOf(lit.values.size()));

		if(maybeType.isNone())
			throw new RuntimeException("element type of array literal could not be deduced");

		Type type = maybeType.unwrap();
		Type typeStripped = TypeHelper.removeConst(type);

		List<Expr> values = new ArrayList<>();

		for(int i = 0; i != lit.values.size(); ++i)
		{
			Expr semaExpr = validate(lit.values.get(i), scope, context, validateDecl, maybeType);
			Maybe<Type> elemType = semaExpr.type().map(TypeHelper::removeConst);

			if(elemType.isNone() || !Type.same(elemType.unwrap(), typeStripped))
			{
				String gotten = elemType.map(Type::toString).or("void");
				String fmt = "element in array literal at index %s has type '%s', expected '%s'";
				throw new RuntimeException(String.format(fmt, i, gotten, typeStripped));
			}

			values.add(semaExpr);
		}

		if(BigInteger.valueOf(values.size()).compareTo(length.unwrap()) != 0)
		{
			String fmt = "invalid number of elements in array literal: got %s, expected %s";
			throw new RuntimeException(String.format(fmt, values.size(), length.unwrap()));
		}

		return new LitArray(length.unwrap(), maybeType.unwrap(), values);
	}

	private Expr visit(katana.ast.expr.LitBool lit, Maybe<Type> deduce)
	{
		return new LitBool(lit.value);
	}

	private void errorLiteralTypeDeduction()
	{
		throw new RuntimeException("type of literal could not be deduced");
	}

	BuiltinType deduceLiteralType(Maybe<Type> maybeType, boolean floatingPoint)
	{
		if(maybeType.isNone())
			errorLiteralTypeDeduction();

		Type type = maybeType.unwrap();

		if(type instanceof Const)
			type = ((Const)type).type;

		if(!(type instanceof Builtin))
			errorLiteralTypeDeduction();

		Builtin builtin = (Builtin)type;

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

	private Expr visit(katana.ast.expr.LitFloat lit, Maybe<Type> deduce)
	{
		if(lit.type.isNone())
			lit.type = Maybe.some(deduceLiteralType(deduce, true));

		if(!Limits.inRange(lit.value, lit.type.unwrap()))
			throw new RuntimeException("floating point literal value is out of range");

		return new LitFloat(lit.value, lit.type.unwrap());
	}

	private Expr visit(katana.ast.expr.LitInt lit, Maybe<Type> deduce)
	{
		if(lit.type.isNone())
			lit.type = Maybe.some(deduceLiteralType(deduce, false));

		if(!Limits.inRange(lit.value, lit.type.unwrap(), context))
			throw new RuntimeException("integer literal value is out of range");

		return new LitInt(lit.value, lit.type.unwrap());
	}

	private Expr visit(katana.ast.expr.LitNull lit, Maybe<Type> deduce)
	{
		return new LitNull();
	}

	private Expr visit(katana.ast.expr.LitString lit, Maybe<Type> deduce)
	{
		return new LitString(lit.value);
	}

	private Expr namedDeclExpr(Decl decl, boolean globalAccess)
	{
		validateDecl.accept(decl);

		if(decl instanceof Global)
		{
			if(!globalAccess)
				throw new RuntimeException("reference to global requires 'global' keyword");

			return new NamedGlobal((Global)decl);
		}

		if(globalAccess)
			throw new RuntimeException("'global' keyword used on reference to symbol that isn't a global");

		if(decl instanceof ExternFunction)
			return new NamedExternFunc((ExternFunction)decl);

		if(decl instanceof Function)
			return new NamedFunc((Function)decl);

		throw new AssertionError("unreachable");
	}

	private Expr visit(katana.ast.expr.MemberAccess memberAccess, Maybe<Type> deduce)
	{
		Expr expr = validate(memberAccess.expr, scope, context, validateDecl, Maybe.none());

		if(expr instanceof NamedRenamedImport)
		{
			RenamedImport import_ = ((NamedRenamedImport)expr).import_;
			Decl decl = import_.decls.get(memberAccess.name);

			if(decl == null)
				throw new RuntimeException(String.format("reference to unknown symbol '%s.%s'", import_.module.path(), memberAccess.name));

			return namedDeclExpr(decl, memberAccess.global);
		}

		if(expr.type().isNone())
			throw new RuntimeException("expression does not result in a value");

		Type type = expr.type().unwrap();

		if(!(type instanceof UserDefined))
			throw new RuntimeException("type is not a data");

		Data data = ((UserDefined)type).data;
		Maybe<Data.Field> field = data.findField(memberAccess.name);

		if(field.isNone())
		{
			String fmt = "data '%s' has no field '%s'";
			throw new RuntimeException(String.format(fmt, data.name(), memberAccess.name));
		}

		if(expr instanceof LValueExpr)
			return new FieldAccessLValue((LValueExpr)expr, field.unwrap());

		return new FieldAccessRValue(expr, field.unwrap());
	}

	private Expr visit(katana.ast.expr.NamedGlobal namedGlobal, Maybe<Type> deduce)
	{
		List<Symbol> candidates = scope.find(namedGlobal.name);

		if(candidates.isEmpty())
			throw new RuntimeException(String.format("reference to unknown symbol '%s'", namedGlobal.name));

		if(candidates.size() > 1)
			throw new RuntimeException(String.format("ambiguos reference to symbol '%s'", namedGlobal.name));

		Symbol symbol = candidates.get(0);

		if(!(symbol instanceof Global))
			throw new RuntimeException(String.format("symbol '%s' does not refer to a global", namedGlobal.name));

		return namedDeclExpr((Decl)symbol, true);
	}

	private Expr visit(NamedSymbol namedSymbol, Maybe<Type> deduce)
	{
		List<Symbol> candidates = scope.find(namedSymbol.name);

		if(candidates.isEmpty())
			throw new RuntimeException(String.format("reference to unknown symbol '%s'", namedSymbol.name));

		if(candidates.size() > 1)
			throw new RuntimeException(String.format("ambiguous reference to symbol '%s'", namedSymbol.name));

		Symbol symbol = candidates.get(0);

		if(symbol instanceof RenamedImport)
			return new NamedRenamedImport((RenamedImport)symbol);

		if(symbol instanceof Decl)
			return namedDeclExpr((Decl)symbol, false);

		if(symbol instanceof Function.Local)
			return new NamedLocal((Function.Local)symbol);

		if(symbol instanceof Function.Param)
			return new NamedParam((Function.Param)symbol);

		throw new RuntimeException(String.format("symbol '%s' does not refer to a value", namedSymbol.name));
	}

	private Expr visit(katana.ast.expr.Offsetof offsetof, Maybe<Type> deduce)
	{
		List<Symbol> candidates = scope.find(offsetof.type);

		if(candidates.isEmpty())
			throw new RuntimeException(String.format("use of unknown type '%s'", offsetof.type));

		if(candidates.size() > 1)
			throw new RuntimeException(String.format("ambiguous reference to symbol '%s'", offsetof.type));

		Symbol symbol = candidates.get(0);

		if(!(symbol instanceof Data))
			throw new RuntimeException(String.format("symbol '%s' does not refer to a type", offsetof.type));

		Maybe<Data.Field> field = ((Data)symbol).findField(offsetof.field);

		if(field.isNone())
			throw new RuntimeException(String.format("data '%s' has no field named '%s'", offsetof.type, offsetof.field));

		return new Offsetof(field.unwrap());
	}

	private Expr visit(katana.ast.expr.Sizeof sizeof, Maybe<Type> deduce)
	{
		return new Sizeof(TypeValidator.validate(sizeof.type, scope, context, validateDecl));
	}
}
