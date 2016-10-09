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
import katana.sema.FileScope;
import katana.sema.FunctionScope;
import katana.sema.OverloadDeclList;
import katana.sema.decl.*;
import katana.sema.expr.Literal;
import katana.sema.type.Type;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class DeclValidator implements IVisitor
{
	private FileScope scope;
	private PlatformContext context;
	private Consumer<Decl> validateDecl;

	private DeclValidator(FileScope scope, PlatformContext context, Consumer<Decl> validateDecl)
	{
		this.scope = scope;
		this.context = context;
		this.validateDecl = validateDecl;
	}

	public static Map<DefinedFunction, StmtValidator> validate(Decl semaDecl, katana.ast.decl.Decl decl, FileScope scope, PlatformContext context, Consumer<Decl> validateDecl)
	{
		DeclValidator validator = new DeclValidator(scope, context, validateDecl);
		Object result = semaDecl.accept(validator, decl);

		if(semaDecl instanceof OverloadSet)
			return (Map<DefinedFunction, StmtValidator>)result;

		return Collections.emptyMap();
	}

	private void visit(Data semaData, katana.ast.decl.Data data)
	{
		for(katana.ast.decl.Data.Field field : data.fields)
		{
			Type type = TypeValidator.validate(field.type, scope, context, validateDecl);

			if(!semaData.defineField(field.name, type))
				throw new RuntimeException(String.format("duplicate field '%s' in data '%s'", field.name, semaData.name()));
		}
	}

	private Maybe<StmtValidator> validateFunction(Function semaFunction, katana.ast.decl.Function function)
	{
		for(katana.ast.decl.Function.Param param : function.params)
		{
			Type type = TypeValidator.validate(param.type, scope, context, validateDecl);

			if(!semaFunction.defineParam(param.name, type))
				throw new RuntimeException(String.format("duplicate parameter name '%s' in function '%s'", param.name, function.name));
		}

		semaFunction.ret = function.ret.map(type -> TypeValidator.validate(type, scope, context, validateDecl));

		if(semaFunction instanceof DefinedFunction)
		{
			DefinedFunction func = (DefinedFunction)semaFunction;
			FunctionScope fscope = new FunctionScope(scope, func);
			return Maybe.some(new StmtValidator(func, fscope, context, validateDecl));
		}

		return Maybe.none();
	}

	private boolean sameSignatures(Function a, Function b)
	{
		if(a.params.size() != b.params.size())
			return false;

		for(int i = 0; i != a.params.size(); ++i)
		{
			Type paramTypeA = a.params.get(i).type;
			Type paramTypeB = b.params.get(i).type;

			if(!TypeHelper.decayedEqual(paramTypeA, paramTypeB))
				return false;
		}

		return true;
	}

	private void checkForDuplicates(OverloadSet set)
	{
		for(int i = 0; i != set.overloads.size(); ++i)
			for(int j = 0; j != i; ++j)
			{
				Function a = set.overloads.get(i);
				Function b = set.overloads.get(j);

				if(sameSignatures(a, b))
					throw new RuntimeException(String.format("duplicate overloads in overload set '%s'", set.name()));
			}

	}

	private Map<DefinedFunction, StmtValidator> visit(OverloadSet set, OverloadDeclList functions)
	{
		Map<DefinedFunction, StmtValidator> validators = new HashMap<>();

		for(int i = 0; i != set.overloads.size(); ++i)
		{
			Function semaFunction = set.overloads.get(i);
			katana.ast.decl.Function function = functions.decls.get(i);
			Maybe<StmtValidator> validator = validateFunction(semaFunction, function);

			if(validator.isSome())
				validators.put((DefinedFunction)semaFunction, validator.unwrap());
		}

		checkForDuplicates(set);

		return validators;
	}

	private void visit(Global semaGlobal, katana.ast.decl.Global global)
	{
		Maybe<Type> maybeDeclaredType = global.type.map(type -> TypeValidator.validate(type, scope, context, validateDecl));
		Maybe<Type> maybeDeclaredTypeDecayed = maybeDeclaredType.map(TypeHelper::decay);
		Literal init = (Literal)ExprValidator.validate(global.init, scope, context, validateDecl, maybeDeclaredTypeDecayed);

		if(init.type().isNone())
			throw new RuntimeException(String.format("initializer for global %s yields void", global.name));

		Type initType = init.type().unwrap();
		Type initTypeDecayed = TypeHelper.decay(initType);
		Type globalType = maybeDeclaredType.or(initTypeDecayed);
		Type globalTypeDecayed = TypeHelper.decay(globalType);

		if(!Type.same(globalTypeDecayed, initTypeDecayed))
		{
			String fmt = "initializer for global '%s' has wrong type: expected '%s', got '%s'";
			throw new RuntimeException(String.format(fmt, semaGlobal.name(), globalTypeDecayed, initTypeDecayed));
		}

		semaGlobal.init = init;
		semaGlobal.type = globalType;
	}

	private void visit(TypeAlias semaAlias, katana.ast.decl.TypeAlias alias)
	{
		semaAlias.type = TypeValidator.validate(alias.type, scope, context, validateDecl);
	}
}
