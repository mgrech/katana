// Copyright 2016-2017 Markus Grech
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
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class StmtCodeGenerator implements IVisitor
{
	private FileCodegenContext context;
	private FunctionCodegenContext fcontext;

	private boolean preceededByTerminator = false;

	public StmtCodeGenerator(FileCodegenContext context, FunctionCodegenContext fcontext)
	{
		this.context = context;
		this.fcontext = fcontext;
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

		for(SemaStmt stmt : compound.body)
			generate(stmt);
	}

	private void visit(SemaStmtExprStmt stmt)
	{
		preceededByTerminator = false;
		ExprCodeGenerator.generate(stmt.expr, context, fcontext);
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
		String condSsa = ExprCodeGenerator.generate(if_.condition, context, fcontext).unwrap();

		GeneratedLabel thenLabel = fcontext.allocateLabel("if.then");
		GeneratedLabel afterLabel = fcontext.allocateLabel("if.after");

		GeneratedLabel firstLabel = if_.negated ? afterLabel : thenLabel;
		GeneratedLabel secondLabel = if_.negated ? thenLabel : afterLabel;

		context.writef("\tbr i1 %s, label %%%s, label %%%s\n\n", condSsa, firstLabel.name, secondLabel.name);
		preceededByTerminator = true;

		visit(thenLabel);
		generate(if_.then);
		visit(afterLabel);

		preceededByTerminator = false;
	}

	private void visit(SemaStmtIfElse ifelse)
	{
		GeneratedLabel after = fcontext.allocateLabel("ifelse.after");

		List<SemaStmt> then = new ArrayList<>();
		then.add(ifelse.then);
		then.add(new GeneratedGoto(after));

		visit(new SemaStmtIf(ifelse.negated, ifelse.condition, new SemaStmtCompound(then)));
		generate(ifelse.else_);
		visit(after);
	}

	private void visit(SemaStmtLoop loop)
	{
		GeneratedLabel label = fcontext.allocateLabel("loop");

		visit(label);
		generate(loop.body);
		visit(new GeneratedGoto(label));
	}

	private void visit(SemaStmtWhile while_)
	{
		GeneratedLabel afterLabel = fcontext.allocateLabel("while.after");

		List<SemaStmt> body = new ArrayList<>();
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

		Maybe<String> returnSsa = ExprCodeGenerator.generate(ret.ret, context, fcontext);

		if(returnSsa.isNone())
		{
			context.write("\tret void\n\n");
			return;
		}

		SemaType type = ret.ret.type();
		String llvmType = TypeCodeGenerator.generate(type, context.platform());
		context.writef("\tret %s %s\n\n", llvmType, returnSsa.unwrap());
	}

	private void visit(SemaStmtNullStmt nullStmt)
	{}
}
