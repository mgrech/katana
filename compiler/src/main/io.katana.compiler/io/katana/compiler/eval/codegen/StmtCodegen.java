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

package io.katana.compiler.eval.codegen;

import io.katana.compiler.eval.exports.UnreachableStmtReachedException;
import io.katana.compiler.sema.stmt.*;
import io.katana.compiler.visitor.IVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("unused")
public class StmtCodegen extends IVisitor
{
	private static StmtCodegen INSTANCE = new StmtCodegen();

	private StmtCodegen() {}

	public static void generate(SemaStmt stmt, MethodContext ctx)
	{
		INSTANCE.invokeSelf(stmt, ctx);
	}

	private void visit(SemaStmtCompound stmt, MethodContext ctx)
	{
		for(var nestedStmt : stmt.bodyStmts)
			invokeSelf(nestedStmt, ctx);
	}

	private void visit(SemaStmtExprStmt stmt, MethodContext ctx)
	{
		ExprCodegen.generate(stmt.nestedExpr, ctx);
	}

	private void visit(SemaStmtGoto stmt, MethodContext ctx)
	{
		ctx.visitor.visitJumpInsn(GOTO, ctx.labels.get(stmt.targetLabel));
	}

	private void visit(SemaStmtIf stmt, MethodContext ctx)
	{
		ExprCodegen.generate(stmt.conditionExpr, ctx);
		var belowLabel = new Label();
		ctx.visitor.visitJumpInsn(stmt.negated ? IFNE : IFEQ, belowLabel);
		generate(stmt.thenStmt, ctx);
		ctx.visitor.visitLabel(belowLabel);
	}

	private void visit(SemaStmtIfElse stmt, MethodContext ctx)
	{
		ExprCodegen.generate(stmt.conditionExpr, ctx);
		var elseLabel = new Label();
		var belowLabel = new Label();
		ctx.visitor.visitJumpInsn(stmt.negated ? IFNE : IFEQ, elseLabel);
		generate(stmt.thenStmt, ctx);
		ctx.visitor.visitJumpInsn(GOTO, belowLabel);
		ctx.visitor.visitLabel(elseLabel);
		generate(stmt.elseStmt, ctx);
		ctx.visitor.visitLabel(belowLabel);
	}

	private void visit(SemaStmtLabel stmt, MethodContext ctx)
	{
		ctx.visitor.visitLabel(ctx.labels.get(stmt));
	}

	private void visit(SemaStmtLoop stmt, MethodContext ctx)
	{
		var topLabel = new Label();
		ctx.visitor.visitLabel(topLabel);
		invokeSelf(stmt.bodyStmt, ctx);
		ctx.visitor.visitJumpInsn(GOTO, topLabel);
	}

	private void visit(SemaStmtNullStmt stmt, MethodContext ctx)
	{}

	private void visit(SemaStmtReturn stmt, MethodContext ctx)
	{
		if(stmt.returnValueExpr.isNone())
			ctx.visitor.visitInsn(RETURN);
		else
		{
			var expr = stmt.returnValueExpr.unwrap();
			ExprCodegen.generate(expr, ctx);

			var type = ctx.universe.classOf(expr.type());

			if(type == boolean.class || type == byte.class || type == short.class || type == int.class)
				ctx.visitor.visitInsn(IRETURN);
			else if(type == long.class)
				ctx.visitor.visitInsn(LRETURN);
			else if(type == float.class)
				ctx.visitor.visitInsn(FRETURN);
			else if(type == double.class)
				ctx.visitor.visitInsn(DRETURN);
			else if(type.isArray())
				ctx.visitor.visitInsn(ARETURN);
			else
				ctx.visitor.visitInsn(LRETURN);
		}
	}

	private void visit(SemaStmtUnreachable stmt, MethodContext ctx)
	{
		var exceptionName = Type.getType(UnreachableStmtReachedException.class).getInternalName();
		ctx.visitor.visitTypeInsn(NEW, exceptionName);
		ctx.visitor.visitInsn(DUP);
		ctx.visitor.visitMethodInsn(INVOKESPECIAL, exceptionName, "<init>", "()V", false);
		ctx.visitor.visitInsn(ATHROW);
	}

	private void visit(SemaStmtWhile stmt, MethodContext ctx)
	{
	}
}
