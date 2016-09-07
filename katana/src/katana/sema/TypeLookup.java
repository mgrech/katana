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

import katana.utils.Maybe;
import katana.backend.PlatformContext;
import katana.sema.decl.Data;
import katana.sema.type.*;
import katana.visitor.IVisitor;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class TypeLookup implements IVisitor
{
	private TypeLookup(Module currentModule)
	{
		this.currentModule = currentModule;
	}

	private Type visit(katana.ast.type.Builtin builtin, katana.sema.decl.Function function, PlatformContext context)
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

	private Type visit(katana.ast.type.Opaque opaque, katana.sema.decl.Function function, PlatformContext context)
	{
		return new Opaque(opaque.size, opaque.alignment);
	}

	private Type visit(katana.ast.type.Array array, katana.sema.decl.Function function, PlatformContext context)
	{
		return new Array(array.size, find(currentModule, array.type, function, context));
	}

	private Type visit(katana.ast.type.Function functionType, katana.sema.decl.Function function, PlatformContext context)
	{
		ArrayList<Type> params = new ArrayList<>();

		for(katana.ast.Type param : functionType.params)
			params.add(find(currentModule, param, function, context));

		Maybe<Type> ret = functionType.ret.map((type) -> find(currentModule, type, function, context));
		return new Function(ret, params);
	}

	private Type visit(katana.ast.type.UserDefined user, katana.sema.decl.Function function, PlatformContext context)
	{
		Maybe<Decl> decl = currentModule.findSymbol(user.name);

		if(decl.isNone())
			throw new RuntimeException("undeclared type '" + user.name + "'");

		if(!(decl.get() instanceof Data))
			throw new RuntimeException("symbol '" + user.name + "' is not a data");

		return new UserDefined((Data)decl.get());
	}

	private Type visit(katana.ast.type.Typeof typeof, katana.sema.decl.Function function, PlatformContext context)
	{
		if(function == null || context == null)
			throw new RuntimeException("typeof is not valid in this context");

		Expr expr = ExprValidator.validate(typeof.expr, function, context);
		return new Typeof(expr);
	}

	private Module currentModule;

	public static Type find(Module currentModule, katana.ast.Type type, katana.sema.decl.Function function, PlatformContext context)
	{
		TypeLookup translator = new TypeLookup(currentModule);
		return (Type)type.accept(translator, function, context);
	}
}
