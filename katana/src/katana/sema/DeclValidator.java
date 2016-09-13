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
import katana.sema.stmt.Stmt;
import katana.sema.type.Type;
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

	public static void validate(Decl semaDecl, katana.ast.decl.Decl decl, FileScope scope, PlatformContext context, Consumer<Decl> validateDecl)
	{
		DeclValidator validator = new DeclValidator(scope, context, validateDecl);
		semaDecl.accept(validator, decl);
	}

	private void visit(Data semaData, katana.ast.decl.Data data)
	{
		for(katana.ast.decl.Data.Field field : data.fields)
		{
			Type type = TypeValidator.validate(field.type, scope, context, validateDecl);

			if(!semaData.defineField(field.name, type))
				throw new RuntimeException("duplicate field '" + field.name + "' in data '" + semaData.name() + "'");
		}
	}

	private void visit(Function semaFunction, katana.ast.decl.Function function)
	{
		for(katana.ast.decl.Function.Param param : function.params)
		{
			Type type = TypeValidator.validate(param.type, scope, context, validateDecl);

			if(!semaFunction.defineParam(param.name, type))
				throw new RuntimeException("duplicate parameter '" + param.name + "' in function '" + function.name + "'");
		}

		FunctionScope fscope = new FunctionScope(scope, semaFunction);

		semaFunction.ret = function.ret.map((type) -> TypeValidator.validate(type, fscope, context, validateDecl));

		StmtValidator validator = new StmtValidator(semaFunction, fscope, context, validateDecl);

		for(katana.ast.stmt.Stmt stmt : function.body)
		{
			Stmt semaStmt = validator.validate(stmt);
			semaFunction.add(semaStmt);
		}

		validator.finalizeValidation();
	}

	private void visit(ExternFunction semaFunction, katana.ast.decl.ExternFunction function)
	{
		for(katana.ast.decl.Function.Param param : function.params)
		{
			Type type = TypeValidator.validate(param.type, scope, context, validateDecl);

			if(!semaFunction.defineParam(type, param.name))
				throw new RuntimeException("duplicate parameter '" + param.name + "' in function '" + function.name + "'");
		}

		semaFunction.ret = function.ret.map((type) -> TypeValidator.validate(type, scope, context, validateDecl));
	}

	private void visit(Global semaGlobal, katana.ast.decl.Global global)
	{
		semaGlobal.type = TypeValidator.validate(global.type, scope, context, validateDecl);
	}
}
