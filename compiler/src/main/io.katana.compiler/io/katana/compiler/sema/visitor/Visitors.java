// Copyright 2019 Markus Grech
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

package io.katana.compiler.sema.visitor;

import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.SemaProgram;
import io.katana.compiler.sema.decl.*;
import io.katana.compiler.sema.expr.SemaExpr;
import io.katana.compiler.sema.stmt.*;
import io.katana.compiler.visitor.IVisitor;

import java.util.function.Function;

public class Visitors
{
	@SuppressWarnings("unused")
	private static class ExprFinder extends IVisitor
	{
		private final IVisitor<SemaExpr> transformer;
		private final Object[] args;

		public ExprFinder(IVisitor<SemaExpr> transformer, Object[] args)
		{
			this.transformer = transformer;
			this.args = new Object[args.length + 1];
			System.arraycopy(args, 0, this.args, 1, args.length);
		}

		public void apply(SemaDecl decl)
		{
			invokeSelf(decl);
		}

		private SemaExpr transformExpr(SemaExpr expr)
		{
			args[0] = expr;
			return transformer.invokeSelf(args);
		}

		private void visit(SemaDeclGlobal decl)
		{
			decl.initializerExpr = decl.initializerExpr.map(this::transformExpr);
		}

		private void visit(SemaDeclOverloadSet decl)
		{
			for(var fdecl : decl.overloads)
				invokeSelf(fdecl);
		}

		private void visit(SemaDeclFunctionDef decl)
		{
			for(var stmt : decl.body)
				invokeSelf(stmt);
		}

		private void visit(SemaDeclExternFunction decl)
		{}

		private void visit(SemaDeclOperator decl)
		{}

		private void visit(SemaDeclStruct decl)
		{}

		private void visit(SemaDeclTypeAlias decl)
		{}

		private void visit(SemaDeclImportedOverloadSet decl)
		{}

		private void visit(SemaDeclRenamedImport decl)
		{}

		private void visit(SemaStmtCompound stmt)
		{
			for(var bodyStmt : stmt.bodyStmts)
				invokeSelf(bodyStmt);
		}

		private void visit(SemaStmtExprStmt stmt)
		{
			stmt.nestedExpr = transformExpr(stmt.nestedExpr);
		}

		private void visit(SemaStmtIf stmt)
		{
			stmt.conditionExpr = transformExpr(stmt.conditionExpr);
			invokeSelf(stmt.thenStmt);
		}

		private void visit(SemaStmtIfElse stmt)
		{
			stmt.conditionExpr = transformExpr(stmt.conditionExpr);
			invokeSelf(stmt.thenStmt);
			invokeSelf(stmt.elseStmt);
		}

		private void visit(SemaStmtLoop stmt)
		{
			invokeSelf(stmt.bodyStmt);
		}

		private void visit(SemaStmtReturn stmt)
		{
			stmt.returnValueExpr = stmt.returnValueExpr.map(this::transformExpr);
		}

		private void visit(SemaStmtGoto stmt)
		{}

		private void visit(SemaStmtLabel stmt)
		{}

		private void visit(SemaStmtNullStmt stmt)
		{}

		private void visit(SemaStmtUnreachable stmt)
		{}
	}

	public static void transformModulesFlat(SemaModule module, Function<SemaModule, SemaModule> action)
	{
		for(var iter = module.children().entrySet().iterator(); iter.hasNext();)
		{
			var entry = iter.next();
			var result = action.apply(entry.getValue());

			if(result == null)
				iter.remove();
			else
				entry.setValue(result);
		}
	}

	private static SemaModule transformModulesDeepChildrenFirst(SemaModule module, Function<SemaModule, SemaModule> transformer)
	{
		for(var child : module.children().values())
			transformModulesDeepChildrenFirst(child, transformer);

		transformModulesFlat(module, transformer);
		return transformer.apply(module);
	}

	public static void transformModulesDeepChildrenFirst(SemaProgram program, Function<SemaModule, SemaModule> transformer)
	{
		program.rootModule = transformModulesDeepChildrenFirst(program.rootModule, transformer);
	}

	public static void transformExprsFlat(SemaProgram program, IVisitor<SemaExpr> transformer, Object... args)
	{
		var visitor = new ExprFinder(transformer, args);

		transformModulesDeepChildrenFirst(program, m -> {
			m.decls().values().forEach(visitor::apply);
			return m;
		});
	}
}
