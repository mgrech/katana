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

import io.katana.compiler.ast.decl.*;
import io.katana.compiler.ast.type.AstTypeBuiltin;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.TypeString;
import io.katana.compiler.sema.decl.*;
import io.katana.compiler.sema.scope.SemaScopeDefinedFunction;
import io.katana.compiler.sema.scope.SemaScopeFile;
import io.katana.compiler.sema.scope.SemaScopeFunction;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

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
		var validator = new DeclIfaceValidator(context, validateDecl);
		semaDecl.accept(validator, info.astDecl, info.scope);
	}

	private void visit(SemaDeclStruct semaStruct, AstDeclStruct struct, SemaScopeFile scope)
	{
		for(var field : struct.fields)
		{
			var type = TypeValidator.validate(field.type, scope, context, validateDecl);

			if(!semaStruct.defineField(field.name, type))
				throw new CompileException(String.format("duplicate field '%s' in type '%s'", field.name, semaStruct.name()));
		}

		var builder = new StructLayoutBuilder(context);

		for(var field : semaStruct.fieldsByIndex())
			builder.appendField(field.type);

		semaStruct.layout = builder.build();
	}

	private void validateFunction(SemaDeclFunction semaFunction, AstDeclFunction function, SemaScopeFile scope)
	{
		if(semaFunction instanceof SemaDeclDefinedFunction)
			semaFunction.scope = new SemaScopeDefinedFunction(scope, (SemaDeclDefinedFunction)semaFunction);
		else
			semaFunction.scope = new SemaScopeFunction(scope, semaFunction);

		for(var param : function.params)
		{
			var type = TypeValidator.validate(param.type, semaFunction.scope, context, validateDecl);

			if(!semaFunction.defineParam(param.name, type))
				throw new CompileException(String.format("duplicate parameter name '%s' in function '%s'", param.name, function.name));
		}

		semaFunction.ret = TypeValidator.validate(function.ret.or(AstTypeBuiltin.VOID), semaFunction.scope, context, validateDecl);
	}

	private boolean sameSignatures(SemaDeclFunction a, SemaDeclFunction b)
	{
		if(a.params.size() != b.params.size())
			return false;

		for(var i = 0; i != a.params.size(); ++i)
		{
			var paramTypeA = Types.removeConst(a.params.get(i).type);
			var paramTypeB = Types.removeConst(b.params.get(i).type);

			if(!Types.equal(paramTypeA, paramTypeB))
				return false;
		}

		return true;
	}

	private void checkForDuplicates(SemaDeclOverloadSet set)
	{
		for(var i = 0; i != set.overloads.size(); ++i)
			for(var j = 0; j != i; ++j)
			{
				var a = set.overloads.get(i);
				var b = set.overloads.get(j);

				if(sameSignatures(a, b))
					throw new CompileException(String.format("duplicate overloads in overload set '%s'", set.qualifiedName()));
			}
	}

	private void visit(SemaDeclOverloadSet semaSet, AstDeclOverloadSet set, SemaScopeFile scope)
	{
		for(var i = 0; i != semaSet.overloads.size(); ++i)
		{
			var semaFunction = semaSet.overloads.get(i);
			var function = set.overloads.get(i);
			validateFunction(semaFunction, function, scope);
		}

		checkForDuplicates(semaSet);
	}

	private void visit(SemaDeclGlobal semaGlobal, AstDeclGlobal global, SemaScopeFile scope)
	{
		var maybeDeclaredType = global.type.map(type -> TypeValidator.validate(type, scope, context, validateDecl));
		var maybeDeclaredTypeNoConst = maybeDeclaredType.map(Types::removeConst);

		if(global.init.isNone())
		{
			if(maybeDeclaredType.isNone())
				throw new CompileException(String.format("global '%s' with 'undef' initializer has no explicit type", global.name));

			semaGlobal.init = Maybe.none();
			semaGlobal.type = maybeDeclaredType.unwrap();
			return;
		}

		var init = ExprValidator.validate(global.init.unwrap(), scope, context, validateDecl, maybeDeclaredTypeNoConst);

		if(Types.isVoid(init.type()))
			throw new CompileException(String.format("initializer for global '%s' yields 'void'", global.name));

		var initTypeNoConst = Types.removeConst(init.type());
		var globalType = maybeDeclaredType.or(initTypeNoConst);
		var globalTypeNoConst = Types.removeConst(globalType);

		if(!Types.equal(globalTypeNoConst, initTypeNoConst))
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
