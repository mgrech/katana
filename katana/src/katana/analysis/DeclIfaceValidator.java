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
import katana.ast.type.AstTypeBuiltin;
import katana.backend.PlatformContext;
import katana.diag.CompileException;
import katana.diag.TypeString;
import katana.sema.decl.*;
import katana.sema.expr.SemaExprLiteral;
import katana.sema.scope.SemaScopeDefinedFunction;
import katana.sema.scope.SemaScopeFile;
import katana.sema.scope.SemaScopeFunction;
import katana.sema.type.SemaType;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class DeclIfaceValidator implements IVisitor
{
	private PlatformContext context;
	private Consumer<SemaDecl> validateDecl;

	private DeclIfaceValidator(PlatformContext context, Consumer<SemaDecl> validateDecl)
	{
		this.context = context;
		this.validateDecl = validateDecl;
	}

	public static void validate(SemaDecl semaDecl, DeclInfo info, PlatformContext context, Consumer<SemaDecl> validateDecl)
	{
		DeclIfaceValidator validator = new DeclIfaceValidator(context, validateDecl);
		semaDecl.accept(validator, info.astDecl, info.scope);
	}

	private void visit(SemaDeclStruct semaStruct, AstDeclStruct struct, SemaScopeFile scope)
	{
		for(AstDeclStruct.Field field : struct.fields)
		{
			SemaType type = TypeValidator.validate(field.type, scope, context, validateDecl);

			if(!semaStruct.defineField(field.name, type))
				throw new CompileException(String.format("duplicate field '%s' in type '%s'", field.name, semaStruct.name()));
		}
	}

	private void validateFunction(SemaDeclFunction semaFunction, AstDeclFunction function, SemaScopeFile scope)
	{
		if(semaFunction instanceof SemaDeclDefinedFunction)
			semaFunction.scope = new SemaScopeDefinedFunction(scope, (SemaDeclDefinedFunction)semaFunction);
		else
			semaFunction.scope = new SemaScopeFunction(scope, semaFunction);

		for(AstDeclFunction.Param param : function.params)
		{
			SemaType type = TypeValidator.validate(param.type, semaFunction.scope, context, validateDecl);

			if(!semaFunction.defineParam(param.name, type))
				throw new CompileException(String.format("duplicate parameter name '%s' in function '%s'", param.name, function.name));
		}

		semaFunction.ret = TypeValidator.validate(function.ret.or(AstTypeBuiltin.VOID), semaFunction.scope, context, validateDecl);
	}

	private boolean sameSignatures(SemaDeclFunction a, SemaDeclFunction b)
	{
		if(a.params.size() != b.params.size())
			return false;

		for(int i = 0; i != a.params.size(); ++i)
		{
			SemaType paramTypeA = TypeHelper.removeConst(a.params.get(i).type);
			SemaType paramTypeB = TypeHelper.removeConst(b.params.get(i).type);

			if(!SemaType.same(paramTypeA, paramTypeB))
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
					throw new CompileException(String.format("duplicate overloads in overload set '%s'", set.qualifiedName()));
			}
	}

	private void visit(SemaDeclOverloadSet semaSet, AstDeclOverloadSet set, SemaScopeFile scope)
	{
		for(int i = 0; i != semaSet.overloads.size(); ++i)
		{
			SemaDeclFunction semaFunction = semaSet.overloads.get(i);
			AstDeclFunction function = set.overloads.get(i);
			validateFunction(semaFunction, function, scope);
		}

		checkForDuplicates(semaSet);
	}

	private void visit(SemaDeclGlobal semaGlobal, AstDeclGlobal global, SemaScopeFile scope)
	{
		Maybe<SemaType> maybeDeclaredType = global.type.map(type -> TypeValidator.validate(type, scope, context, validateDecl));
		Maybe<SemaType> maybeDeclaredTypeNoConst = maybeDeclaredType.map(TypeHelper::removeConst);

		if(global.init.isNone())
		{
			if(maybeDeclaredType.isNone())
				throw new CompileException(String.format("global '%s' with 'undef' initializer has no explicit type", global.name));

			semaGlobal.init = Maybe.none();
			semaGlobal.type = maybeDeclaredType.unwrap();
			return;
		}

		SemaExprLiteral init = (SemaExprLiteral)ExprValidator.validate(global.init.unwrap(), scope, context, validateDecl, maybeDeclaredTypeNoConst);

		if(TypeHelper.isVoidType(init.type()))
			throw new CompileException(String.format("initializer for global %s yields 'void'", global.name));

		SemaType initTypeNoConst = TypeHelper.removeConst(init.type());
		SemaType globalType = maybeDeclaredType.or(initTypeNoConst);
		SemaType globalTypeNoConst = TypeHelper.removeConst(globalType);

		if(!SemaType.same(globalTypeNoConst, initTypeNoConst))
		{
			String fmt = "initializer for global '%s' has wrong type: expected '%s', got '%s'";
			throw new CompileException(String.format(fmt, semaGlobal.name(), TypeString.of(globalTypeNoConst), TypeString.of(initTypeNoConst)));
		}

		semaGlobal.init = Maybe.some(init);
		semaGlobal.type = globalType;
	}

	private void visit(SemaDeclTypeAlias semaAlias, AstDeclTypeAlias alias, SemaScopeFile scope)
	{
		semaAlias.type = TypeValidator.validate(alias.type, scope, context, validateDecl);
	}

	private void visit(SemaDeclOperator semaDecl, AstDeclOperator decl, SemaScopeFile scope)
	{}
}
