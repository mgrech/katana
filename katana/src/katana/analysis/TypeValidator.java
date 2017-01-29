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

import katana.ast.type.*;
import katana.backend.PlatformContext;
import katana.diag.CompileException;
import katana.sema.SemaSymbol;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclStruct;
import katana.sema.decl.SemaDeclTypeAlias;
import katana.sema.expr.SemaExpr;
import katana.sema.scope.SemaScope;
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
		TypeValidator translator = new TypeValidator(scope, context, validateDecl);
		return (SemaType)type.accept(translator);
	}

	private SemaType visit(AstTypeBuiltin builtin)
	{
		switch(builtin.which)
		{
		case VOID:    return SemaTypeBuiltin.VOID;
		case BYTE:    return SemaTypeBuiltin.BYTE;
		case BOOL:    return SemaTypeBuiltin.BOOL;
		case INT8:    return SemaTypeBuiltin.INT8;
		case INT16:   return SemaTypeBuiltin.INT16;
		case INT32:   return SemaTypeBuiltin.INT32;
		case INT64:   return SemaTypeBuiltin.INT64;
		case INT:     return SemaTypeBuiltin.INT;
		case UINT8:   return SemaTypeBuiltin.UINT8;
		case UINT16:  return SemaTypeBuiltin.UINT16;
		case UINT32:  return SemaTypeBuiltin.UINT32;
		case UINT64:  return SemaTypeBuiltin.UINT64;
		case UINT:    return SemaTypeBuiltin.UINT;
		case FLOAT32: return SemaTypeBuiltin.FLOAT32;
		case FLOAT64: return SemaTypeBuiltin.FLOAT64;
		case NULL:    return SemaTypeBuiltin.NULL;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private SemaType visit(AstTypeOpaque opaque)
	{
		return new SemaTypeOpaque(opaque.size, opaque.alignment);
	}

	private SemaType visit(AstTypeArray array)
	{
		if(array.length.compareTo(BigInteger.ZERO) == -1)
			throw new CompileException(String.format("negative array length: %s", array.length));

		return new SemaTypeArray(array.length, validate(array.type, scope, context, validateDecl));
	}

	private SemaType visit(AstTypeFunction functionType)
	{
		List<SemaType> params = new ArrayList<>();

		for(AstType param : functionType.params)
			params.add(validate(param, scope, context, validateDecl));

		Maybe<SemaType> ret = functionType.ret.map(type -> validate(type, scope, context, validateDecl));
		return new SemaTypeFunction(ret.or(SemaTypeBuiltin.VOID), params);
	}

	private SemaType visit(AstTypeUserDefined user)
	{
		List<SemaSymbol> candidates = scope.find(user.name);

		if(candidates.isEmpty())
			throw new CompileException(String.format("use of unknown type '%s'", user.name));

		if(candidates.size() > 1)
			throw new CompileException(String.format("ambiguous reference to symbol '%s'", user.name));

		SemaSymbol symbol = candidates.get(0);

		if(symbol instanceof SemaDecl)
			validateDecl.accept((SemaDecl)symbol);

		if(symbol instanceof SemaDeclTypeAlias)
			return ((SemaDeclTypeAlias)symbol).type;

		if(symbol instanceof SemaDeclStruct)
			return new SemaTypeUserDefined((SemaDeclStruct)symbol);

		throw new CompileException(String.format("symbol '%s' does not refer to a type", symbol.name()));
	}

	private SemaType visit(AstTypeConst const_)
	{
		SemaType type = validate(const_.type, scope, context, validateDecl);

		if(type instanceof SemaTypeFunction)
			throw new CompileException("forming const function type");

		return Types.addConst(type);
	}

	private SemaType visit(AstTypeTypeof typeof)
	{
		if(scope == null)
			throw new CompileException("'typeof' is not valid in this context");

		SemaExpr expr = ExprValidator.validate(typeof.expr, scope, context, validateDecl, Maybe.none());
		return expr.type();
	}

	private SemaType visit(AstTypeNullablePointer pointer)
	{
		return new SemaTypeNullablePointer(validate(pointer.type, scope, context, validateDecl));
	}

	private SemaType visit(AstTypeNonNullablePointer pointer)
	{
		return new SemaTypeNonNullablePointer(validate(pointer.type, scope, context, validateDecl));
	}
}
