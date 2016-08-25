package katana.backend.llvm;

import katana.backend.PlatformContext;
import katana.sema.Stmt;
import katana.sema.Type;
import katana.sema.stmt.*;
import katana.visitor.IVisitor;

@SuppressWarnings("unused")
public class StmtCodeGen implements IVisitor
{
	private StmtCodeGen() {}

	private void visit(Compound compound, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		for(Stmt stmt : compound.body)
			apply(stmt, builder, context, fcontext);
	}

	private void visit(ExprStmt stmt, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		ExprCodeGen.apply(stmt.expr, builder, context, fcontext);
	}

	private void visit(Goto goto_, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		builder.append(String.format("\tbr label %%%s\n\n", goto_.target.name));
		int after = fcontext.nextTemporary();
		builder.append(String.format("; %%%s:\n", after));
	}

	private void visit(If if_, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		String expr = ExprCodeGen.apply(if_.condition, builder, context, fcontext);
		int then = fcontext.nextTemporary();

		StringBuilder tempThen = new StringBuilder();
		tempThen.append(String.format("; %%%s:\n", then));
		StmtCodeGen.apply(if_.then, tempThen, context, fcontext);

		int afterThen = fcontext.nextTemporary();
		int afterIfElse = afterThen;

		StringBuilder tempOtherwise = new StringBuilder();

		if(if_.otherwise.isSome())
		{
			tempOtherwise.append(String.format("; %%%s:\n", afterThen));
			StmtCodeGen.apply(if_.otherwise.unwrap(), tempOtherwise, context, fcontext);
			int afterOtherwise = fcontext.nextTemporary();
			tempThen.append(String.format("\tbr label %%%s\n\n", afterOtherwise));
			afterIfElse = afterOtherwise;
		}

		builder.append(String.format("\tbr i1 %s, label %%%s, label %%%s\n\n", expr, then, afterThen));
		builder.append(tempThen.toString());
		builder.append(tempOtherwise.toString());
		builder.append(String.format("; %%%s:\n", afterIfElse));
	}

	private void visit(Label label, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		builder.append(label.name);
		builder.append(":\n");
	}

	private void visit(Return ret, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		if(ret.ret.isNone())
		{
			builder.append("\tret void\n\n");
			return;
		}

		String expr = ExprCodeGen.apply(ret.ret.unwrap(), builder, context, fcontext);
		Type type = ret.ret.unwrap().type().unwrap();
		String llvmType = TypeCodeGen.apply(type, context);
		builder.append(String.format("\tret %s %s\n\n", llvmType, expr));

	}

	public static void apply(Stmt stmt, StringBuilder builder, PlatformContext context, FunctionContext fcontext)
	{
		StmtCodeGen visitor = new StmtCodeGen();
		stmt.accept(visitor, builder, context, fcontext);
	}
}
