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

import io.katana.compiler.ast.type.*;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.sema.decl.SemaDecl;
import io.katana.compiler.sema.decl.SemaDeclStruct;
import io.katana.compiler.sema.decl.SemaDeclTypeAlias;
import io.katana.compiler.sema.scope.SemaScope;
import io.katana.compiler.sema.type.*;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.util.ArrayList;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class TypeValidator implements IVisitor
{
	private SemaScope scope;
	private PlatformContext context;
	private Consumer<SemaDecl> validateDecl;

	private TypeValidator(SemaScope scope, PlatformContext context, Consumer<SemaDecl> validateDecl)
	{
		this.scope = scope;
		this.context = context;
		this.validateDecl = validateDecl;
	}

	public static SemaType validate(AstType type, SemaScope scope, PlatformContext context, Consumer<SemaDecl> validateDecl)
	{
		var validator = new TypeValidator(scope, context, validateDecl);
		return (SemaType)type.accept(validator);
	}

	private SemaType visit(AstTypeBuiltin builtin)
	{
		return SemaTypeBuiltin.of(builtin.which);
	}

	private SemaType visit(AstTypeTuple tuple)
	{
		var builder = new StructLayoutBuilder(context);
		var types = new ArrayList<SemaType>();

		for(var type : tuple.fieldTypes)
		{
			var semaType = validate(type, scope, context, validateDecl);
			types.add(semaType);
			builder.appendField(semaType);
		}

		return new SemaTypeTuple(types, builder.build());
	}

	private SemaType visit(AstTypeSlice slice)
	{
		return Types.addSlice(validate(slice.elementType, scope, context, validateDecl));
	}

	private SemaType visit(AstTypeArray array)
	{
		if(array.length < 0)
			throw new CompileException(String.format("negative array length: %s", array.length));

		return Types.addArray(array.length, validate(array.elementType, scope, context, validateDecl));
	}

	private SemaType visit(AstTypeFunction functionType)
	{
		var params = new ArrayList<SemaType>();

		for(var param : functionType.paramTypes)
			params.add(validate(param, scope, context, validateDecl));

		var ret = functionType.returnType.map(type -> validate(type, scope, context, validateDecl));
		return new SemaTypeFunction(ret.or(SemaTypeBuiltin.VOID), params);
	}

	private SemaType visit(AstTypeUserDefined user)
	{
		var candidates = scope.find(user.name);

		if(candidates.isEmpty())
			throw new CompileException(String.format("use of unknown type '%s'", user.name));

		if(candidates.size() > 1)
			throw new CompileException(String.format("ambiguous reference to symbol '%s'", user.name));

		var symbol = candidates.get(0);

		if(symbol instanceof SemaDecl)
			validateDecl.accept((SemaDecl)symbol);

		if(symbol instanceof SemaDeclTypeAlias)
			return ((SemaDeclTypeAlias)symbol).aliasedType;

		if(symbol instanceof SemaDeclStruct)
			return new SemaTypeStruct((SemaDeclStruct)symbol);

		throw new CompileException(String.format("symbol '%s' does not refer to a type", symbol.name()));
	}

	private SemaType visit(AstTypeConst const_)
	{
		var type = validate(const_.nestedType, scope, context, validateDecl);

		if(type instanceof SemaTypeFunction)
			throw new CompileException("forming const function type");

		return Types.addConst(type);
	}

	private SemaType visit(AstTypeTypeof typeof)
	{
		if(scope == null)
			throw new CompileException("'typeof' is not valid in this context");

		var expr = ExprValidator.validate(typeof.nestedExpr, scope, context, validateDecl, Maybe.none());
		return expr.type();
	}

	private SemaType visit(AstTypeNullablePointer pointer)
	{
		return Types.addNullablePointer(validate(pointer.pointeeType, scope, context, validateDecl));
	}

	private SemaType visit(AstTypeNonNullablePointer pointer)
	{
		return Types.addNonNullablePointer(validate(pointer.pointeeType, scope, context, validateDecl));
	}
}
