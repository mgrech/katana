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

package katana.backend.llvm;

import katana.backend.PlatformContext;
import katana.sema.decl.DefinedFunction;
import katana.sema.stmt.*;
import katana.sema.type.Type;
import katana.visitor.IVisitor;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class StmtCodeGenerator implements IVisitor
{
	private StringBuilder builder;
	private PlatformContext context;
	private FunctionContext fcontext;

	private boolean preceededByTerminator = false;

	public StmtCodeGenerator(StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		this.builder = builder;
		this.context = context;
		this.fcontext = fcontext;
	}

	public void generate(Stmt stmt)
	{
		stmt.accept(this);
	}

	public void finish(DefinedFunction func)
	{
		if(!preceededByTerminator)
			if(func.ret.isNone())
				builder.append("\tret void\n");
			else
				throw new RuntimeException(String.format("at least one path in '%s' returns no value", func.qualifiedName()));
	}

	private void visit(Compound compound)
	{
		preceededByTerminator = false;

		for(Stmt stmt : compound.body)
			generate(stmt);
	}

	private void visit(ExprStmt stmt)
	{
		preceededByTerminator = false;
		ExprCodeGenerator.generate(stmt.expr, builder, context, fcontext);
	}

	private void generateGoto(String label)
	{
		preceededByTerminator = true;
		builder.append(String.format("\tbr label %%%s\n\n", label));
	}

	private void visit(Goto goto_)
	{
		generateGoto("ul$" + goto_.target.name);
	}

	private void visit(GeneratedGoto goto_)
	{
		generateGoto(goto_.label.name);
	}

	private void visit(If if_)
	{
		String condSSA = ExprCodeGenerator.generate(if_.condition, builder, context, fcontext).unwrap();

		GeneratedLabel thenLabel = fcontext.allocateLabel("if.then");
		GeneratedLabel afterLabel = fcontext.allocateLabel("if.after");

		GeneratedLabel firstLabel = if_.negated ? afterLabel : thenLabel;
		GeneratedLabel secondLabel = if_.negated ? thenLabel : afterLabel;

		builder.append(String.format("\tbr i1 %s, label %%%s, label %%%s\n\n", condSSA, firstLabel.name, secondLabel.name));
		preceededByTerminator = true;

		visit(thenLabel);
		generate(if_.then);
		visit(afterLabel);

		preceededByTerminator = false;
	}

	private void visit(IfElse ifelse)
	{
		GeneratedLabel after = fcontext.allocateLabel("ifelse.after");

		List<Stmt> then = new ArrayList<>();
		then.add(ifelse.then);
		then.add(new GeneratedGoto(after));

		visit(new If(ifelse.negated, ifelse.condition, new Compound(then)));
		generate(ifelse.else_);
		visit(after);
	}

	private void visit(Loop loop)
	{
		GeneratedLabel label = fcontext.allocateLabel("loop");

		visit(label);
		generate(loop.body);
		visit(new GeneratedGoto(label));
	}

	private void visit(While while_)
	{
		GeneratedLabel afterLabel = fcontext.allocateLabel("while.after");

		List<Stmt> body = new ArrayList<>();
		body.add(new If(!while_.negated, while_.condition, new GeneratedGoto(afterLabel)));
		body.add(while_.body);

		visit(new Loop(new Compound(body)));
		visit(afterLabel);
	}

	private void generateLabel(String name)
	{
		if(!preceededByTerminator)
			builder.append(String.format("\tbr label %%%s\n\n", name));

		builder.append(String.format("%s:\n", name));
		preceededByTerminator = false;
	}

	private void visit(Label label)
	{
		generateLabel("ul$" + label.name);
	}

	private void visit(GeneratedLabel label)
	{
		generateLabel(label.name);
	}

	private void visit(Return ret)
	{
		preceededByTerminator = true;

		if(ret.ret.isNone())
		{
			builder.append("\tret void\n\n");
			return;
		}

		String expr = ExprCodeGenerator.generate(ret.ret.unwrap(), builder, context, fcontext).unwrap();
		Type type = ret.ret.unwrap().type().unwrap();
		String llvmType = TypeCodeGenerator.generate(type, context);
		builder.append(String.format("\tret %s %s\n\n", llvmType, expr));
	}
}
