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

package io.katana.compiler.backend.llvm;

import io.katana.compiler.analysis.Types;
import io.katana.compiler.sema.decl.SemaDeclDefinedFunction;
import io.katana.compiler.sema.stmt.*;
import io.katana.compiler.visitor.IVisitor;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class StmtCodeGenerator implements IVisitor
{
	private FunctionCodegenContext context;

	private boolean preceededByTerminator = false;

	public StmtCodeGenerator(FunctionCodegenContext context)
	{
		this.context = context;
	}

	public void generate(SemaStmt stmt)
	{
		stmt.accept(this);
	}

	public void finish(SemaDeclDefinedFunction func)
	{
		if(!preceededByTerminator)
		{
			if(Types.isZeroSized(func.ret))
				context.write("\tret void\n");
			else
				context.write("\tunreachable\n");
		}
	}

	private void visit(SemaStmtCompound compound)
	{
		preceededByTerminator = false;

		for(var stmt : compound.body)
			generate(stmt);
	}

	private void visit(SemaStmtExprStmt stmt)
	{
		preceededByTerminator = false;
		ExprCodeGenerator.generate(stmt.expr, context);
	}

	private void generateGoto(String label)
	{
		preceededByTerminator = true;
		context.writef("\tbr label %%%s\n\n", label);
	}

	private void visit(SemaStmtGoto goto_)
	{
		generateGoto("ul$" + goto_.target.name);
	}

	private void visit(GeneratedGoto goto_)
	{
		generateGoto(goto_.label.name);
	}

	private void visit(SemaStmtIf if_)
	{
		var condSsa = ExprCodeGenerator.generate(if_.condition, context).unwrap();

		var thenLabel = context.allocateLabel("if.then");
		var afterLabel = context.allocateLabel("if.after");

		var firstLabel = if_.negated ? afterLabel : thenLabel;
		var secondLabel = if_.negated ? thenLabel : afterLabel;

		context.writef("\tbr i1 %s, label %%%s, label %%%s\n\n", condSsa, firstLabel.name, secondLabel.name);
		preceededByTerminator = true;

		visit(thenLabel);
		generate(if_.then);
		visit(afterLabel);

		preceededByTerminator = false;
	}

	private void visit(SemaStmtIfElse ifelse)
	{
		var after = context.allocateLabel("ifelse.after");

		var then = new ArrayList<SemaStmt>();
		then.add(ifelse.then);
		then.add(new GeneratedGoto(after));

		visit(new SemaStmtIf(ifelse.negated, ifelse.condition, new SemaStmtCompound(then)));
		generate(ifelse.else_);
		visit(after);
	}

	private void visit(SemaStmtLoop loop)
	{
		var label = context.allocateLabel("loop");

		visit(label);
		generate(loop.body);
		visit(new GeneratedGoto(label));
	}

	private void visit(SemaStmtWhile while_)
	{
		var afterLabel = context.allocateLabel("while.after");

		var body = new ArrayList<SemaStmt>();
		body.add(new SemaStmtIf(!while_.negated, while_.condition, new GeneratedGoto(afterLabel)));
		body.add(while_.body);

		visit(new SemaStmtLoop(new SemaStmtCompound(body)));
		visit(afterLabel);
	}

	private void generateLabel(String name)
	{
		if(!preceededByTerminator)
			context.writef("\tbr label %%%s\n\n", name);

		context.writef("%s:\n", name);
		preceededByTerminator = false;
	}

	private void visit(SemaStmtLabel label)
	{
		generateLabel("ul$" + label.name);
	}

	private void visit(GeneratedLabel label)
	{
		generateLabel(label.name);
	}

	private void visit(SemaStmtReturn ret)
	{
		preceededByTerminator = true;

		var returnSsa = ExprCodeGenerator.generate(ret.ret, context);

		if(returnSsa.isNone())
		{
			context.write("\tret void\n\n");
			return;
		}

		var type = ret.ret.type();
		var llvmType = TypeCodeGenerator.generate(type, context.platform());
		context.writef("\tret %s %s\n\n", llvmType, returnSsa.unwrap());
	}

	private void visit(SemaStmtNullStmt nullStmt)
	{}
}
