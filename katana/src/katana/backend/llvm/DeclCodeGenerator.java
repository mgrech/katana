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
public class DeclCodeGenerator implements IVisitor
{
	private StringBuilder builder;
	private PlatformContext context;

	public DeclCodeGenerator(StringBuilder builder, PlatformContext context)
	{
		this.builder = builder;
		this.context = context;
	}

	public void generate(Decl decl)
	{
		decl.accept(this);
	}

	private static String qualifiedName(Decl decl)
	{
		Path modulePath = decl.module().path();
		return modulePath.toString() + "." + decl.name();
	}

	private void visit(Data data)
	{
		builder.append('%');
		builder.append(qualifiedName(data));
		builder.append(" = type { ");

		List<Data.Field> fields = data.fieldsByIndex();

		if(!fields.isEmpty())
		{
			builder.append(TypeCodeGenerator.generate(fields.get(0).type, context));

			for(int i = 1; i != fields.size(); ++i)
			{
				builder.append(", ");
				builder.append(TypeCodeGenerator.generate(fields.get(i).type, context));
			}
		}

		builder.append(" }\n");
	}

	private void visit(Function function)
	{
		builder.append("define ");
		builder.append(function.ret.map((type) -> TypeCodeGenerator.generate(type, context)).or("void"));
		builder.append(" @");
		builder.append(qualifiedName(function));
		builder.append('(');

		if(!function.params.isEmpty())
		{
			Function.Param first = function.params.get(0);

			builder.append(TypeCodeGenerator.generate(first.type, context));
			builder.append(" %p$");
			builder.append(first.name);

			for(int i = 1; i != function.params.size(); ++i)
			{
				builder.append(", ");

				Function.Param param = function.params.get(i);
				builder.append(TypeCodeGenerator.generate(param.type, context));
				builder.append(" %p$");
				builder.append(param.name);
			}
		}

		builder.append(")\n{\n");

		for(Function.Param param : function.params)
		{
			String typeString = TypeCodeGenerator.generate(param.type, context);
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
			String llvmType = TypeCodeGenerator.generate(type, context);
			int align = type.alignof(context);
			builder.append(String.format("\t%%%s = alloca %s, align %s\n", entry.getKey(), llvmType, align));
		}

		if(!function.locals.isEmpty())
			builder.append('\n');

		StmtCodeGenerator stmtCodeGen = new StmtCodeGenerator(builder, context, fcontext);

		for(Stmt stmt : function.body)
			stmtCodeGen.generate(stmt);

		stmtCodeGen.finish(function);

		builder.append("}\n");
	}

	private void visit(ExternFunction externFunction)
	{
		String retTypeString = externFunction.ret.map((t) -> TypeCodeGenerator.generate(t, context)).or("void");
		builder.append(String.format("declare %s @%s(", retTypeString, externFunction.externName));

		if(!externFunction.params.isEmpty())
		{
			builder.append(TypeCodeGenerator.generate(externFunction.params.get(0).type, context));

			for(int i = 1; i != externFunction.params.size(); ++i)
			{
				String typeString = TypeCodeGenerator.generate(externFunction.params.get(i).type, context);
				builder.append(", ");
				builder.append(typeString);
			}
		}

		builder.append(")\n");
	}

	private void visit(Global global)
	{
		String qualifiedName = qualifiedName(global);
		String typeString = TypeCodeGenerator.generate(global.type, context);
		builder.append(String.format("@%s = private global %s zeroinitializer\n", qualifiedName, typeString));
	}
}
