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
import katana.sema.decl.Data;
import katana.sema.decl.ExternFunction;
import katana.sema.decl.Function;
import katana.sema.decl.Global;
import katana.sema.expr.*;
import katana.sema.type.Array;
import katana.sema.type.Builtin;
import katana.sema.type.UserDefined;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("unused")
public class ExprValidator implements IVisitor
{
	private Function function;
	private PlatformContext context;

	private ExprValidator(Function function, PlatformContext context)
	{
		this.function = function;
		this.context = context;
	}

	public static Expr validate(katana.ast.Expr expr, Function function, PlatformContext context)
	{
		ExprValidator validator = new ExprValidator(function, context);
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
		Expr expr = validate(addressof.expr, function, context);

		if(!(expr instanceof LValueExpr))
			throw new RuntimeException("addressof() requires lvalue operand");

		((LValueExpr)expr).useAsLValue(true);
		return new Addressof(expr);
	}

	private Expr visit(katana.ast.expr.Alignof alignof)
	{
		return new Alignof(TypeLookup.find(function.module(), alignof.type, function, context));
	}

	private Expr visit(katana.ast.expr.ArrayAccess arrayAccess)
	{
		Expr value = validate(arrayAccess.value, function, context);
		Expr index = validate(arrayAccess.index, function, context);
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
		Expr left = validate(assign.left, function, context);
		Expr right = validate(assign.right, function, context);

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

		((LValueExpr)left).useAsLValue(true);
		return new Assign(left, right);
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
			Expr semaExpr = validate(builtinCall.args.get(i), function, context);
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
		Expr expr = validate(deref.expr, function, context);

		if(expr.type().isNone() || expr.type().unwrap() != Builtin.PTR)
			throw new RuntimeException("expression of type ptr expected in deref");

		return new Deref(TypeLookup.find(function.module(), deref.type, function, context), expr);
	}

	private Expr visit(katana.ast.expr.FunctionCall call)
	{
		Expr expr = validate(call.expr, function, context);

		if(expr.type().isNone() || !(expr.type().unwrap() instanceof katana.sema.type.Function))
			throw new RuntimeException("expression does not result in function type");

		List<Expr> args = new ArrayList<>();

		for(katana.ast.Expr e : call.args)
			args.add(validate(e, function, context));

		katana.sema.type.Function type = (katana.sema.type.Function)expr.type().unwrap();
		checkArguments(type.params, args);

		if(expr instanceof NamedFunc)
			return new DirectFunctionCall(((NamedFunc)expr).func, args, call.inline);

		return new IndirectFunctionCall(expr, args);
	}

	private Expr visit(katana.ast.expr.LitBool litBool)
	{
		return new LitBool(litBool.value);
	}

	private Expr visit(katana.ast.expr.LitFloat litFloat)
	{
		return new LitFloat(litFloat.value, litFloat.type);
	}

	private Expr visit(katana.ast.expr.LitInt litInt)
	{
		return new LitInt(litInt.value, litInt.type);
	}

	private Expr visit(katana.ast.expr.LitNull litNull)
	{
		return new LitNull();
	}

	private Expr visit(katana.ast.expr.LitString litString)
	{
		return new LitString(litString.value);
	}

	private Expr visit(katana.ast.expr.MemberAccess memberAccess)
	{
		Expr expr = validate(memberAccess.expr, function, context);

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
		Maybe<Function.Local> maybeLocal = function.findLocal(namedValue.name);

		if(maybeLocal.isSome())
			return new NamedLocal(maybeLocal.unwrap());

		Maybe<Function.Param> maybeParam = function.findParam(namedValue.name);

		if(maybeParam.isSome())
			return new NamedParam(maybeParam.unwrap());

		Maybe<Decl> maybeDecl = function.module().findSymbol(namedValue.name);

		if(maybeDecl.isNone())
			throw new RuntimeException("unknown symbol '" + namedValue.name + "'");

		Decl decl = maybeDecl.unwrap();

		if(decl instanceof ExternFunction)
			return new NamedExternFunc((ExternFunction)decl);

		if(decl instanceof Function)
			return new NamedFunc((Function)decl);

		if(decl instanceof Global)
			return new NamedGlobal((Global)decl);

		throw new RuntimeException("symbol '" + namedValue.name + "' does not refer to a value");
	}

	private Expr visit(katana.ast.expr.Offsetof offsetof)
	{
		Maybe<Decl> decl = function.module().findSymbol(offsetof.type);

		if(decl.isNone())
			throw new RuntimeException("type '" + offsetof.type + "' not found");

		Decl d = decl.unwrap();

		if(!(d instanceof Data))
			throw new RuntimeException("'" + offsetof.type + "' is not a data");

		Maybe<Data.Field> field = ((Data)d).findField(offsetof.field);

		if(field.isNone())
			throw new RuntimeException("field '" + offsetof.field + "' not found");

		return new Offsetof(field.unwrap());
	}

	private Expr visit(katana.ast.expr.Sizeof sizeof)
	{
		return new Sizeof(TypeLookup.find(function.module(), sizeof.type, function, context));
	}
}
