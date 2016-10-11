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

import katana.ast.decl.*;
import katana.backend.PlatformContext;
import katana.sema.decl.*;
import katana.sema.expr.SemaExprLiteral;
import katana.sema.scope.SemaScopeFile;
import katana.sema.scope.SemaScopeFunction;
import katana.sema.type.SemaType;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class DeclValidator implements IVisitor
{
	private SemaScopeFile scope;
	private PlatformContext context;
	private Consumer<SemaDecl> validateDecl;

	private DeclValidator(SemaScopeFile scope, PlatformContext context, Consumer<SemaDecl> validateDecl)
	{
		this.scope = scope;
		this.context = context;
		this.validateDecl = validateDecl;
	}

	public static Map<SemaDeclDefinedFunction, StmtValidator> validate(SemaDecl semaDecl, AstDecl decl, SemaScopeFile scope, PlatformContext context, Consumer<SemaDecl> validateDecl)
	{
		DeclValidator validator = new DeclValidator(scope, context, validateDecl);
		Object result = semaDecl.accept(validator, decl);

		if(semaDecl instanceof SemaDeclOverloadSet)
			return (Map<SemaDeclDefinedFunction, StmtValidator>)result;

		return Collections.emptyMap();
	}

	private void visit(SemaDeclData semaData, AstDeclData data)
	{
		for(AstDeclData.Field field : data.fields)
		{
			SemaType type = TypeValidator.validate(field.type, scope, context, validateDecl);

			if(!semaData.defineField(field.name, type))
				throw new RuntimeException(String.format("duplicate field '%s' in type '%s'", field.name, semaData.name()));
		}
	}

	private Maybe<StmtValidator> validateFunction(SemaDeclFunction semaFunction, AstDeclFunction function)
	{
		for(AstDeclFunction.Param param : function.params)
		{
			SemaType type = TypeValidator.validate(param.type, scope, context, validateDecl);

			if(!semaFunction.defineParam(param.name, type))
				throw new RuntimeException(String.format("duplicate parameter name '%s' in function '%s'", param.name, function.name));
		}

		semaFunction.ret = function.ret.map(type -> TypeValidator.validate(type, scope, context, validateDecl));

		if(semaFunction instanceof SemaDeclDefinedFunction)
		{
			SemaDeclDefinedFunction func = (SemaDeclDefinedFunction)semaFunction;
			SemaScopeFunction fscope = new SemaScopeFunction(scope, func);
			return Maybe.some(new StmtValidator(func, fscope, context, validateDecl));
		}

		return Maybe.none();
	}

	private boolean sameSignatures(SemaDeclFunction a, SemaDeclFunction b)
	{
		if(a.params.size() != b.params.size())
			return false;

		for(int i = 0; i != a.params.size(); ++i)
		{
			SemaType paramTypeA = a.params.get(i).type;
			SemaType paramTypeB = b.params.get(i).type;

			if(!TypeHelper.decayedEqual(paramTypeA, paramTypeB))
				return false;
		}

		return true;
	}

	private void checkForDuplicates(SemaDeclOverloadSet set)
	{
		for(int i = 0; i != set.overloads.size(); ++i)
			for(int j = 0; j != i; ++j)
			{
				SemaDeclFunction a = set.overloads.get(i);
				SemaDeclFunction b = set.overloads.get(j);

				if(sameSignatures(a, b))
					throw new RuntimeException(String.format("duplicate overloads in overload set '%s'", set.qualifiedName()));
			}

	}

	private Map<SemaDeclDefinedFunction, StmtValidator> visit(SemaDeclOverloadSet set, OverloadDeclList functions)
	{
		Map<SemaDeclDefinedFunction, StmtValidator> validators = new HashMap<>();

		for(int i = 0; i != set.overloads.size(); ++i)
		{
			SemaDeclFunction semaFunction = set.overloads.get(i);
			AstDeclFunction function = functions.decls.get(i);
			Maybe<StmtValidator> validator = validateFunction(semaFunction, function);

			if(validator.isSome())
				validators.put((SemaDeclDefinedFunction)semaFunction, validator.unwrap());
		}

		checkForDuplicates(set);

		return validators;
	}

	private void visit(SemaDeclGlobal semaGlobal, AstDeclGlobal global)
	{
		Maybe<SemaType> maybeDeclaredType = global.type.map(type -> TypeValidator.validate(type, scope, context, validateDecl));
		Maybe<SemaType> maybeDeclaredTypeDecayed = maybeDeclaredType.map(TypeHelper::decay);
		SemaExprLiteral init = (SemaExprLiteral)ExprValidator.validate(global.init, scope, context, validateDecl, maybeDeclaredTypeDecayed);

		if(init.type().isNone())
			throw new RuntimeException(String.format("initializer for global %s yields 'void'", global.name));

		SemaType initType = init.type().unwrap();
		SemaType initTypeDecayed = TypeHelper.decay(initType);
		SemaType globalType = maybeDeclaredType.or(initTypeDecayed);
		SemaType globalTypeDecayed = TypeHelper.decay(globalType);

		if(!SemaType.same(globalTypeDecayed, initTypeDecayed))
		{
			String fmt = "initializer for global '%s' has wrong type: expected '%s', got '%s'";
			throw new RuntimeException(String.format(fmt, semaGlobal.name(), globalTypeDecayed, initTypeDecayed));
		}

		semaGlobal.init = init;
		semaGlobal.type = globalType;
	}

	private void visit(SemaDeclTypeAlias semaAlias, AstDeclTypeAlias alias)
	{
		semaAlias.type = TypeValidator.validate(alias.type, scope, context, validateDecl);
	}
}
