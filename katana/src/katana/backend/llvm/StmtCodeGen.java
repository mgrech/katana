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
import katana.sema.Stmt;
import katana.sema.Type;
import katana.sema.decl.Function;
import katana.sema.stmt.*;
import katana.visitor.IVisitor;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class StmtCodeGen implements IVisitor
{
	private void visit(Compound compound, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		preceededByTerminator = false;

		for(Stmt stmt : compound.body)
			apply(stmt, builder, context, fcontext);
	}

	private void visit(ExprStmt stmt, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		preceededByTerminator = false;
		ExprCodeGen.apply(stmt.expr, builder, context, fcontext);
	}

	private void generateGoto(String label, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		preceededByTerminator = true;
		builder.append(String.format("\tbr label %%%s\n\n", label));
	}

	private void visit(Goto goto_, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		generateGoto("ul$" + goto_.target.name, builder, context, fcontext);
	}

	private void visit(GeneratedGoto goto_, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		generateGoto(goto_.label.name, builder, context, fcontext);
	}

	private void visit(If if_, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		String condSSA = ExprCodeGen.apply(if_.condition, builder, context, fcontext).unwrap();

		GeneratedLabel thenLabel = fcontext.allocateLabel("if.then");
		GeneratedLabel afterLabel = fcontext.allocateLabel("if.after");

		GeneratedLabel firstLabel = if_.negated ? afterLabel : thenLabel;
		GeneratedLabel secondLabel = if_.negated ? thenLabel : afterLabel;

		builder.append(String.format("\tbr i1 %s, label %%%s, label %%%s\n\n", condSSA, firstLabel.name, secondLabel.name));
		preceededByTerminator = true;

		apply(thenLabel, builder, context, fcontext);

		apply(if_.then, builder, context, fcontext);
		apply(afterLabel, builder, context, fcontext);

		preceededByTerminator = false;
	}

	private void visit(IfElse ifelse, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		GeneratedLabel after = fcontext.allocateLabel("ifelse.after");

		List<Stmt> then = new ArrayList<>();
		then.add(ifelse.then);
		then.add(new GeneratedGoto(after));

		visit(new If(ifelse.negated, ifelse.condition, new Compound(then)), builder, context, fcontext);
		apply(ifelse.else_, builder, context, fcontext);
		visit(after, builder, context, fcontext);
	}

	private void visit(Loop loop, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		GeneratedLabel label = fcontext.allocateLabel("loop");

		visit(label, builder, context, fcontext);
		apply(loop.body, builder, context, fcontext);
		visit(new GeneratedGoto(label), builder, context, fcontext);
	}

	private void visit(While while_, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		GeneratedLabel afterLabel = fcontext.allocateLabel("while.after");

		List<Stmt> body = new ArrayList<>();
		body.add(new If(!while_.negated, while_.condition, new GeneratedGoto(afterLabel)));
		body.add(while_.body);

		visit(new Loop(new Compound(body)), builder, context, fcontext);
		visit(afterLabel, builder, context, fcontext);
	}

	private void generateLabel(String name, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		if(!preceededByTerminator)
			builder.append(String.format("\tbr label %%%s\n\n", name));

		builder.append(String.format("%s:\n", name));
		preceededByTerminator = false;
	}

	private void visit(Label label, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		generateLabel("ul$" + label.name, builder, context, fcontext);
	}

	private void visit(GeneratedLabel label, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		generateLabel(label.name, builder, context, fcontext);
	}

	private void visit(Return ret, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		preceededByTerminator = true;

		if(ret.ret.isNone())
		{
			builder.append("\tret void\n\n");
			return;
		}

		String expr = ExprCodeGen.apply(ret.ret.unwrap(), builder, context, fcontext).unwrap();
		Type type = ret.ret.unwrap().type().unwrap();
		String llvmType = TypeCodeGen.apply(type, context);
		builder.append(String.format("\tret %s %s\n\n", llvmType, expr));
	}

	public void apply(Stmt stmt, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		stmt.accept(this, builder, context, fcontext);
	}

	public void finish(Function func, StringBuilder builder)
	{
		if(!preceededByTerminator)
			if(func.ret.isNone())
				builder.append("\tret void\n");
			else
				throw new RuntimeException(String.format("at least one path in '%s' returns no value", func.qualifiedName()));
	}

	private boolean preceededByTerminator = false;
}
