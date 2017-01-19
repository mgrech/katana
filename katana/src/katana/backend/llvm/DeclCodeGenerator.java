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

package katana.backend.llvm;

import katana.analysis.TypeAlignment;
import katana.analysis.TypeHelper;
import katana.ast.AstPath;
import katana.backend.PlatformContext;
import katana.project.Project;
import katana.sema.decl.*;
import katana.sema.stmt.SemaStmt;
import katana.sema.type.SemaType;
import katana.sema.type.SemaTypePointer;
import katana.visitor.IVisitor;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class DeclCodeGenerator implements IVisitor
{
	private StringBuilder builder;
	private Project project;
	private PlatformContext context;

	public DeclCodeGenerator(StringBuilder builder, Project project, PlatformContext context)
	{
		this.builder = builder;
		this.project = project;
		this.context = context;
	}

	public void generate(SemaDecl decl)
	{
		decl.accept(this);
	}

	private static String qualifiedName(SemaDecl decl)
	{
		AstPath modulePath = decl.module().path();
		return modulePath.toString() + "." + decl.name();
	}

	private void visit(SemaDeclStruct struct)
	{
		builder.append('%');
		builder.append(qualifiedName(struct));
		builder.append(" = type { ");

		List<SemaDeclStruct.Field> fields = struct.fieldsByIndex();

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

	private void generateParam(SemaDeclFunction.Param param, boolean isExternal)
	{
		builder.append(TypeCodeGenerator.generate(param.type, context));

		if(param.type instanceof SemaTypePointer)
			builder.append(" nonnull");

		if(!isExternal)
		{
			builder.append(" %p$");
			builder.append(param.name);
		}
	}

	private void generateSignature(SemaDeclFunction function)
	{
		boolean isExternal = function instanceof SemaDeclExternFunction;

		if(isExternal)
			builder.append("declare ");

		else
		{
			builder.append("define ");

			if(function.exported)
			{
				switch(project.type)
				{
				case LIBRARY: builder.append("dllexport "); break;
				case EXECUTABLE: break;
				default: throw new AssertionError("unreachable");
				}
			}

			else
				builder.append("private ");
		}

		builder.append(TypeCodeGenerator.generate(function.ret, context));
		builder.append(" @");

		if(isExternal)
			builder.append(((SemaDeclExternFunction)function).externName);
		else
			builder.append(FunctionNameMangler.mangle(function));

		builder.append('(');

		if(!function.params.isEmpty())
		{
			SemaDeclFunction.Param first = function.params.get(0);
			generateParam(first, isExternal);

			for(int i = 1; i != function.params.size(); ++i)
			{
				builder.append(", ");

				SemaDeclFunction.Param param = function.params.get(i);
				generateParam(param, isExternal);
			}
		}

		builder.append(")\n");
	}

	private void generateFunctionBody(SemaDeclDefinedFunction function)
	{
		builder.append("{\n");

		for(SemaDeclFunction.Param param : function.params)
		{
			String typeString = TypeCodeGenerator.generate(param.type, context);
			BigInteger alignment = TypeAlignment.of(param.type, context);
			builder.append(String.format("\t%%%s = alloca %s, align %s\n", param.name, typeString, alignment));
			builder.append(String.format("\tstore %s %%p$%s, %s* %%%s\n", typeString, param.name, typeString, param.name));
		}

		if(!function.params.isEmpty())
			builder.append('\n');

		FunctionContext fcontext = new FunctionContext();

		for(Map.Entry<String, SemaDeclDefinedFunction.Local> entry : function.localsByName.entrySet())
		{
			SemaType type = entry.getValue().type;
			String llvmType = TypeCodeGenerator.generate(type, context);
			BigInteger align = TypeAlignment.of(type, context);
			builder.append(String.format("\t%%%s = alloca %s, align %s\n", entry.getKey(), llvmType, align));
		}

		if(!function.locals.isEmpty())
			builder.append('\n');

		StmtCodeGenerator stmtCodeGen = new StmtCodeGenerator(builder, context, fcontext);

		for(SemaStmt stmt : function.body)
			stmtCodeGen.generate(stmt);

		stmtCodeGen.finish(function);

		builder.append("}\n");
	}

	private void visit(SemaDeclOverloadSet set)
	{
		for(SemaDeclFunction overload : set.overloads)
		{
			if(overload instanceof SemaDeclDefinedFunction)
			{
				generateSignature(overload);
				generateFunctionBody((SemaDeclDefinedFunction)overload);
			}

			else if(overload instanceof SemaDeclExternFunction)
				generateSignature(overload);

			else
				throw new AssertionError("unreachable");
		}
	}

	private void visit(SemaDeclGlobal global)
	{
		String qualifiedName = qualifiedName(global);
		String typeString = TypeCodeGenerator.generate(global.type, context);
		String initializerString = global.init.map(i -> ExprCodeGenerator.generate(i, builder, context, null).unwrap()).or("zeroinitializer");
		String kind = TypeHelper.isConst(global.type) ? "constant" : "global";
		builder.append(String.format("@%s = private %s %s %s\n", qualifiedName, kind, typeString, initializerString));
	}

	private void visit(SemaDeclTypeAlias alias)
	{}

	private void visit(SemaDeclOperator operator)
	{}
}
