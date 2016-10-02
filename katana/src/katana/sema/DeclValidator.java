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
import katana.sema.expr.Literal;
import katana.sema.type.Type;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

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

	public static Maybe<StmtValidator> validate(Decl semaDecl, katana.ast.decl.Decl decl, FileScope scope, PlatformContext context, Consumer<Decl> validateDecl)
	{
		DeclValidator validator = new DeclValidator(scope, context, validateDecl);
		Object result = semaDecl.accept(validator, decl);

		if(semaDecl instanceof Function)
			return Maybe.some((StmtValidator)result);

		return Maybe.none();
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

	private StmtValidator visit(Function semaFunction, katana.ast.decl.Function function)
	{
		for(katana.ast.decl.Function.Param param : function.params)
		{
			Type type = TypeValidator.validate(param.type, scope, context, validateDecl);

			if(!semaFunction.defineParam(param.name, type))
				throw new RuntimeException(String.format("duplicate parameter name '%s' in function '%s'", param.name, function.name));
		}

		FunctionScope fscope = new FunctionScope(scope, semaFunction);
		semaFunction.ret = function.ret.map((type) -> TypeValidator.validate(type, fscope, context, validateDecl));
		return new StmtValidator(semaFunction, fscope, context, validateDecl);
	}

	private void visit(ExternFunction semaFunction, katana.ast.decl.ExternFunction function)
	{
		for(katana.ast.decl.Function.Param param : function.params)
		{
			Type type = TypeValidator.validate(param.type, scope, context, validateDecl);

			if(!semaFunction.defineParam(type, param.name))
				throw new RuntimeException(String.format("duplicate parameter name '%s' in function '%s'", param.name, function.name));
		}

		semaFunction.ret = function.ret.map((type) -> TypeValidator.validate(type, scope, context, validateDecl));
	}

	private void visit(Global semaGlobal, katana.ast.decl.Global global)
	{
		Maybe<Type> maybeDeclaredType = global.type.map((t) -> TypeValidator.validate(t, scope, context, validateDecl));
		Maybe<Type> maybeDeclaredTypeStripped = maybeDeclaredType.map(TypeHelper::removeConst);
		Literal init = (Literal)ExprValidator.validate(global.init, scope, context, validateDecl, maybeDeclaredTypeStripped);

		if(init.type().isNone())
			throw new RuntimeException(String.format("initializer for global %s yields void", global.name));

		Type initType = init.type().unwrap();
		Type initTypeStripped = TypeHelper.removeConst(initType);
		Type globalType = maybeDeclaredType.or(initTypeStripped);
		Type globalTypeStripped = TypeHelper.removeConst(globalType);

		if(!Type.same(globalTypeStripped, initTypeStripped))
		{
			String fmt = "initializer for global '%s' has wrong type: expected '%s', got '%s'";
			throw new RuntimeException(String.format(fmt, semaGlobal.name(), globalTypeStripped, initTypeStripped));
		}

		semaGlobal.init = init;
		semaGlobal.type = globalType;
	}

	private void visit(TypeAlias semaAlias, katana.ast.decl.TypeAlias alias)
	{
		semaAlias.type = TypeValidator.validate(alias.type, scope, context, validateDecl);
	}
}
