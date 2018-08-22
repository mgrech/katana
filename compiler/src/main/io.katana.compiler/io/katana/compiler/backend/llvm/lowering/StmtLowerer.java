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

package io.katana.compiler.backend.llvm.lowering;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.backend.llvm.FileCodegenContext;
import io.katana.compiler.backend.llvm.GeneratedGoto;
import io.katana.compiler.backend.llvm.ir.IrLabel;
import io.katana.compiler.backend.llvm.ir.decl.IrFunctionBuilder;
import io.katana.compiler.backend.llvm.ir.type.IrType;
import io.katana.compiler.backend.llvm.ir.type.IrTypes;
import io.katana.compiler.backend.llvm.ir.value.IrValue;
import io.katana.compiler.sema.decl.SemaDeclDefinedFunction;
import io.katana.compiler.sema.expr.SemaExpr;
import io.katana.compiler.sema.stmt.*;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class StmtLowerer implements IVisitor
{
	private final FileCodegenContext context;
	private final IrFunctionBuilder builder;

	private boolean preceededByTerminator = false;

	public StmtLowerer(FileCodegenContext context, IrFunctionBuilder builder)
	{
		this.context = context;
		this.builder = builder;
	}

	public void lower(SemaStmt stmt)
	{
		stmt.accept(this);
	}

	public void finish(SemaDeclDefinedFunction func)
	{
		if(!preceededByTerminator)
		{
			if(Types.isZeroSized(func.ret))
				builder.ret(IrTypes.VOID, Maybe.none());
			else
				builder.unreachable();
		}
	}

	private Maybe<IrValue> lower(SemaExpr expr)
	{
		return ExprLowerer.lower(expr, context, builder);
	}

	private IrType lower(SemaType type)
	{
		return TypeLowerer.lower(type, context.platform());
	}

	private void visit(SemaStmtCompound compound)
	{
		preceededByTerminator = false;

		for(var stmt : compound.body)
			lower(stmt);
	}

	private void visit(SemaStmtExprStmt stmt)
	{
		preceededByTerminator = false;
		lower(stmt.expr);
	}

	private void generateGoto(IrLabel label)
	{
		preceededByTerminator = true;
		builder.br(label);
	}

	private void visit(SemaStmtGoto goto_)
	{
		generateGoto(IrLabel.of(goto_.target.name));
	}

	private void visit(GeneratedGoto goto_)
	{
		generateGoto(IrLabel.of(goto_.label.name));
	}

	private void visit(SemaStmtIf if_)
	{
		var condition = lower(if_.condition).unwrap();

		var thenLabel = builder.allocateLabel("if.then");
		var afterLabel = builder.allocateLabel("if.after");

		var trueLabel = if_.negated ? afterLabel : thenLabel;
		var falseLabel = if_.negated ? thenLabel : afterLabel;

		builder.br(condition, trueLabel, falseLabel);
		preceededByTerminator = true;

		generateLabel(thenLabel);
		lower(if_.then);
		generateLabel(afterLabel);

		preceededByTerminator = false;
	}

	private void visit(SemaStmtIfElse ifelse)
	{
		var after = builder.allocateLabel("ifelse.after");

		var then = new ArrayList<SemaStmt>();
		then.add(ifelse.then);
		then.add(new GeneratedGoto(after));

		visit(new SemaStmtIf(ifelse.negated, ifelse.condition, new SemaStmtCompound(then)));
		lower(ifelse.else_);
		generateLabel(after);
	}

	private void visit(SemaStmtLoop loop)
	{
		var label = builder.allocateLabel("loop");
		generateLabel(label);
		lower(loop.body);
		builder.br(label);
	}

	private void visit(SemaStmtWhile while_)
	{
		var afterLabel = builder.allocateLabel("while.after");

		var body = new ArrayList<SemaStmt>();
		body.add(new SemaStmtIf(!while_.negated, while_.condition, new GeneratedGoto(afterLabel)));
		body.add(while_.body);

		visit(new SemaStmtLoop(new SemaStmtCompound(body)));
		generateLabel(afterLabel);
	}

	private void generateLabel(IrLabel label)
	{
		if(!preceededByTerminator)
			builder.br(label);

		builder.label(label.name);
		preceededByTerminator = false;
	}

	private void visit(SemaStmtLabel label)
	{
		generateLabel(IrLabel.of(label.name));
	}

	private void visit(SemaStmtReturn ret)
	{
		preceededByTerminator = true;
		var type = ret.ret.map(SemaExpr::type).map(this::lower).or(IrTypes.VOID);
		var value = ret.ret.map(this::lower).unwrap();
		builder.ret(type, value);
	}

	private void visit(SemaStmtNullStmt nullStmt)
	{}

	private void visit(SemaStmtUnreachable unreachable)
	{
		preceededByTerminator = true;
		builder.unreachable();
	}
}
