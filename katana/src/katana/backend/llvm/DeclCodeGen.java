package katana.backend.llvm;

import katana.ast.Path;
import katana.backend.PlatformContext;
import katana.sema.Decl;
import katana.sema.Stmt;
import katana.sema.Type;
import katana.sema.decl.Data;
import katana.sema.decl.ExternFunction;
import katana.sema.decl.Function;
import katana.sema.decl.Global;
import katana.visitor.IVisitor;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class DeclCodeGen implements IVisitor
{
	private DeclCodeGen() {}

	private static String qualifiedName(Decl decl)
	{
		Path modulePath = decl.module().path();
		return modulePath.toString() + "." + decl.name();
	}

	private void visit(Data data, StringBuilder builder, PlatformContext context)
	{
		builder.append('%');
		builder.append(qualifiedName(data));
		builder.append(" = type { ");

		List<Data.Field> fields = data.fieldsByIndex();

		if(!fields.isEmpty())
		{
			builder.append(TypeCodeGen.apply(fields.get(0).type, context));

			for(int i = 1; i != fields.size(); ++i)
			{
				builder.append(", ");
				builder.append(TypeCodeGen.apply(fields.get(i).type, context));
			}
		}

		builder.append(" }\n");
	}

	private void visit(Function function, StringBuilder builder, PlatformContext context)
	{
		builder.append("define ");
		builder.append(function.ret.map((type) -> TypeCodeGen.apply(type, context)).or("void"));
		builder.append(" @");
		builder.append(qualifiedName(function));
		builder.append('(');

		if(!function.params.isEmpty())
		{
			Function.Param first = function.params.get(0);

			builder.append(TypeCodeGen.apply(first.type, context));
			builder.append(" %p$");
			builder.append(first.name);

			for(int i = 1; i != function.params.size(); ++i)
			{
				builder.append(", ");

				Function.Param param = function.params.get(i);
				builder.append(TypeCodeGen.apply(param.type, context));
				builder.append(" %p$");
				builder.append(param.name);
			}
		}

		builder.append(")\n{\n");

		for(Function.Param param : function.params)
		{
			String typeString = TypeCodeGen.apply(param.type, context);
			int alignment = param.type.alignof(context);
			builder.append(String.format("\t%%%s = alloca %s, align %s\n", param.name, typeString, alignment));
			builder.append(String.format("\tstore %s %%p$%s, %s* %%%s\n", typeString, param.name, typeString, param.name));
		}

		if(!function.params.isEmpty())
			builder.append('\n');

		FunctionContext fcontext = new FunctionContext();

		for(Map.Entry<String, Function.Local> entry : function.localsByName.entrySet())
		{
			Type type = entry.getValue().type;
			String llvmType = TypeCodeGen.apply(type, context);
			int align = type.alignof(context);
			builder.append(String.format("\t%%%s = alloca %s, align %s\n", entry.getKey(), llvmType, align));
		}

		if(!function.locals.isEmpty())
			builder.append('\n');

		StmtCodeGen stmtCodeGen = new StmtCodeGen();

		for(Stmt stmt : function.body)
			stmtCodeGen.apply(stmt, builder, context, fcontext);

		stmtCodeGen.finish(function, builder);

		builder.append("}\n");
	}

	private void visit(ExternFunction externFunction, StringBuilder builder, PlatformContext context)
	{
		String retTypeString = externFunction.ret.map((t) -> TypeCodeGen.apply(t, context)).or("void");
		builder.append(String.format("declare %s @%s(", retTypeString, externFunction.externName));

		if(!externFunction.params.isEmpty())
		{
			builder.append(TypeCodeGen.apply(externFunction.params.get(0).type, context));

			for(int i = 1; i != externFunction.params.size(); ++i)
			{
				String typeString = TypeCodeGen.apply(externFunction.params.get(i).type, context);
				builder.append(", ");
				builder.append(typeString);
			}
		}

		builder.append(")\n");
	}

	private void visit(Global global, StringBuilder builder, PlatformContext context)
	{
		String qualifiedName = qualifiedName(global);
		String typeString = TypeCodeGen.apply(global.type, context);
		builder.append(String.format("@%s = private global %s zeroinitializer\n", qualifiedName, typeString));
	}

	public static void apply(Decl decl, StringBuilder builder, PlatformContext context)
	{
		DeclCodeGen visitor = new DeclCodeGen();
		decl.accept(visitor, builder, context);
	}
}
