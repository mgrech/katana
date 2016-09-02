package katana.backend.llvm;

import katana.backend.PlatformContext;
import katana.sema.Stmt;
import katana.sema.Type;
import katana.sema.decl.Function;
import katana.sema.stmt.*;
import katana.visitor.IVisitor;

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

	private void visit(Goto goto_, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		preceededByTerminator = true;
		builder.append(String.format("\tbr label %%%s\n\n", goto_.target.name));
	}

	private void visit(If if_, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		preceededByTerminator = false;

		String exprSSA = ExprCodeGen.apply(if_.condition, builder, context, fcontext).unwrap();
		String thenSSA = fcontext.allocateSSA();

		StringBuilder tempThen = new StringBuilder();
		tempThen.append(String.format("; %s:\n", thenSSA));
		apply(if_.then, tempThen, context, fcontext);

		String afterThenSSA = fcontext.allocateSSA();

		builder.append(String.format("\tbr i1 %s, label %s, label %s\n\n", exprSSA, thenSSA, afterThenSSA));
		builder.append(tempThen.toString());
		builder.append(String.format("; %s:\n", afterThenSSA));
	}

	private void visit(Label label, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		if(!preceededByTerminator)
			builder.append(String.format("\tbr label %%%s\n\n", label.name));

		builder.append(String.format("%s:\n", label.name));
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
