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

import katana.backend.PlatformContext;
import katana.sema.Scope;
import katana.sema.Symbol;
import katana.sema.decl.Data;
import katana.sema.decl.Decl;
import katana.sema.decl.TypeAlias;
import katana.sema.expr.Expr;
import katana.sema.type.*;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class TypeValidator implements IVisitor
{
	private Scope scope;
	private PlatformContext context;
	private Consumer<Decl> validateDecl;

	private TypeValidator(Scope scope, PlatformContext context, Consumer<Decl> validateDecl)
	{
		this.scope = scope;
		this.context = context;
		this.validateDecl = validateDecl;
	}

	public static Type validate(katana.ast.type.Type type, Scope scope, PlatformContext context, Consumer<Decl> validateDecl)
	{
		TypeValidator translator = new TypeValidator(scope, context, validateDecl);
		return (Type)type.accept(translator);
	}

	private Type visit(katana.ast.type.Builtin builtin)
	{
		switch(builtin.which)
		{
		case INT8:    return Builtin.INT8;
		case UINT8:   return Builtin.UINT8;
		case INT16:   return Builtin.INT16;
		case UINT16:  return Builtin.UINT16;
		case INT32:   return Builtin.INT32;
		case UINT32:  return Builtin.UINT32;
		case INT64:   return Builtin.INT64;
		case UINT64:  return Builtin.UINT64;
		case INT:     return Builtin.INT;
		case UINT:    return Builtin.UINT;
		case PINT:    return Builtin.PINT;
		case UPINT:   return Builtin.UPINT;
		case PTR:     return Builtin.PTR;
		case BOOL:    return Builtin.BOOL;
		case FLOAT32: return Builtin.FLOAT32;
		case FLOAT64: return Builtin.FLOAT64;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private Type visit(katana.ast.type.Opaque opaque)
	{
		return new Opaque(opaque.size, opaque.alignment);
	}

	private Type visit(katana.ast.type.Array array)
	{
		if(array.length.compareTo(BigInteger.ZERO) == -1)
			throw new RuntimeException(String.format("invalid array length %s", array.length));

		return new Array(array.length, validate(array.type, scope, context, validateDecl));
	}

	private Type visit(katana.ast.type.Function functionType)
	{
		ArrayList<Type> params = new ArrayList<>();

		for(katana.ast.type.Type param : functionType.params)
			params.add(validate(param, scope, context, validateDecl));

		Maybe<Type> ret = functionType.ret.map(type -> validate(type, scope, context, validateDecl));
		return new Function(ret, params);
	}

	private Type visit(katana.ast.type.UserDefined user)
	{
		List<Symbol> candidates = scope.find(user.name);

		if(candidates.isEmpty())
			throw new RuntimeException(String.format("use of unknown type '%s'", user.name));

		if(candidates.size() > 1)
			throw new RuntimeException(String.format("ambiguous reference to symbol '%s'", user.name));

		Symbol symbol = candidates.get(0);

		if(symbol instanceof Decl)
			validateDecl.accept((Decl)symbol);

		if(symbol instanceof TypeAlias)
			return ((TypeAlias)symbol).type;

		if(symbol instanceof Data)
			return new UserDefined((Data)symbol);

		throw new RuntimeException(String.format("symbol '%s' does not refer to a type"));
	}

	private Type visit(katana.ast.type.Const const_)
	{
		Type type = validate(const_.type, scope, context, validateDecl);

		if(type instanceof Const)
			return type;

		if(type instanceof Array)
			throw new RuntimeException("forming const array type, did you mean array of const element type?");

		if(type instanceof Function)
			throw new RuntimeException("forming const function type");

		return new Const(type);
	}

	private Type visit(katana.ast.type.Typeof typeof)
	{
		if(scope == null)
			throw new RuntimeException("typeof is not valid in this context");

		Expr expr = ExprValidator.validate(typeof.expr, scope, context, validateDecl, Maybe.none());
		Maybe<Type> type = expr.type();

		if(type.isNone())
			throw new RuntimeException("expression passed to typeof yields no type");

		return type.unwrap();
	}
}
