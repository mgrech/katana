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

import katana.backend.PlatformContext;
import katana.sema.decl.*;
import katana.sema.expr.*;
import katana.sema.type.Array;
import katana.sema.type.Builtin;
import katana.sema.type.Type;
import katana.sema.type.UserDefined;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

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

	public static Expr validate(katana.ast.expr.Expr expr, Scope scope, PlatformContext context, Consumer<Decl> validateDecl)
	{
		ExprValidator validator = new ExprValidator(scope, context, validateDecl);
		return (Expr)expr.accept(validator);
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

	private Expr visit(katana.ast.expr.Addressof addressof)
	{
		Expr expr = validate(addressof.expr, scope, context, validateDecl);

		if(!(expr instanceof LValueExpr))
			throw new RuntimeException("addressof() requires lvalue operand");

		LValueExpr lvalue = (LValueExpr)expr;
		lvalue.useAsLValue(true);
		return new Addressof(lvalue);
	}

	private Expr visit(katana.ast.expr.Alignof alignof)
	{
		return new Alignof(TypeValidator.validate(alignof.type, scope, context, validateDecl));
	}

	private Expr visit(katana.ast.expr.ArrayAccess arrayAccess)
	{
		Expr value = validate(arrayAccess.value, scope, context, validateDecl);
		Expr index = validate(arrayAccess.index, scope, context, validateDecl);
		Maybe<Type> indexType = index.type();

		if(indexType.isNone() || indexType.unwrap() != Builtin.INT)
			throw new RuntimeException("array access requires index of type int");

		if(value.type().isNone() || !(value.type().unwrap() instanceof Array))
			throw new RuntimeException("array access requires expression yielding array type");

		if(value instanceof LValueExpr)
			return new ArrayAccessLValue((LValueExpr)value, index);

		return new ArrayAccessRValue(value, index);
	}

	private Expr visit(katana.ast.expr.Assign assign)
	{
		Expr left = validate(assign.left, scope, context, validateDecl);
		Expr right = validate(assign.right, scope, context, validateDecl);

		if(left.type().isNone())
			throw new RuntimeException("value expected on left side of assignment");

		if(right.type().isNone())
			throw new RuntimeException("value expected on right side of assignment");

		if(!(left instanceof LValueExpr))
			throw new RuntimeException("assignment requires lvalue operand on left side");

		if(!Type.same(left.type().unwrap(), right.type().unwrap()))
			throw new RuntimeException("same types expected in assignment");

		if(left.type().unwrap() instanceof katana.sema.type.Function)
			throw new RuntimeException("cannot assign to function");

		LValueExpr leftAsLvalue = (LValueExpr)left;
		leftAsLvalue.useAsLValue(true);
		return new Assign(leftAsLvalue, right);
	}

	private Expr visit(katana.ast.expr.BuiltinCall builtinCall)
	{
		Maybe<BuiltinFunc> maybeFunc = context.findBuiltin(builtinCall.name);

		if(maybeFunc.isNone())
			throw new RuntimeException(String.format("builtin %s not found", builtinCall.name));

		List<Expr> args = new ArrayList<>();
		List<Type> types = new ArrayList<>();

		for(int i = 0; i != builtinCall.args.size(); ++i)
		{
			Expr semaExpr = validate(builtinCall.args.get(i), scope, context, validateDecl);
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

	private Expr visit(katana.ast.expr.Deref deref)
	{
		Expr expr = validate(deref.expr, scope, context, validateDecl);

		if(expr.type().isNone() || expr.type().unwrap() != Builtin.PTR)
			throw new RuntimeException("expression of type ptr expected in deref");

		return new Deref(TypeValidator.validate(deref.type, scope, context, validateDecl), expr);
	}

	private Expr visit(katana.ast.expr.FunctionCall call)
	{
		Expr expr = validate(call.expr, scope, context, validateDecl);

		if(expr.type().isNone() || !(expr.type().unwrap() instanceof katana.sema.type.Function))
			throw new RuntimeException("expression does not result in function type");

		List<Expr> args = new ArrayList<>();

		for(katana.ast.expr.Expr e : call.args)
			args.add(validate(e, scope, context, validateDecl));

		katana.sema.type.Function type = (katana.sema.type.Function)expr.type().unwrap();
		checkArguments(type.params, args);

		if(expr instanceof NamedFunc)
			return new DirectFunctionCall(((NamedFunc)expr).func, args, call.inline);

		return new IndirectFunctionCall(expr, args);
	}

	private Expr visit(katana.ast.expr.LitBool lit)
	{
		return new LitBool(lit.value);
	}

	private Expr visit(katana.ast.expr.LitFloat lit)
	{
		return new LitFloat(lit.value, lit.type);
	}

	private Expr visit(katana.ast.expr.LitInt lit)
	{
		return new LitInt(lit.value, lit.type);
	}

	private Expr visit(katana.ast.expr.LitNull lit)
	{
		return new LitNull();
	}

	private Expr visit(katana.ast.expr.LitString lit)
	{
		return new LitString(lit.value);
	}

	private Expr visit(katana.ast.expr.MemberAccess memberAccess)
	{
		Expr expr = validate(memberAccess.expr, scope, context, validateDecl);

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

	private Expr visit(katana.ast.expr.NamedValue namedValue)
	{
		List<Symbol> candidates = scope.find(namedValue.name);

		if(candidates.isEmpty())
			throw new RuntimeException(String.format("use of unknown symbol '%s'", namedValue.name));

		if(candidates.size() > 1)
			throw new RuntimeException(String.format("ambiguous reference to symbol '%s'", namedValue.name));

		Symbol symbol = candidates.get(0);

		if(symbol instanceof Decl)
			validateDecl.accept((Decl)symbol);

		if(symbol instanceof Function.Local)
			return new NamedLocal((Function.Local)symbol);

		if(symbol instanceof Function.Param)
			return new NamedParam((Function.Param)symbol);

		if(symbol instanceof ExternFunction)
			return new NamedExternFunc((ExternFunction)symbol);

		if(symbol instanceof Function)
			return new NamedFunc((Function)symbol);

		if(symbol instanceof Global)
			return new NamedGlobal((Global)symbol);

		throw new RuntimeException(String.format("symbol '%s' does not refer to a value", namedValue.name));
	}

	private Expr visit(katana.ast.expr.Offsetof offsetof)
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

	private Expr visit(katana.ast.expr.Sizeof sizeof)
	{
		return new Sizeof(TypeValidator.validate(sizeof.type, scope, context, validateDecl));
	}
}
