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
import io.katana.compiler.ast.AstPath;
import io.katana.compiler.sema.decl.*;
import io.katana.compiler.sema.stmt.SemaStmt;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.sema.type.SemaTypeNonNullablePointer;
import io.katana.compiler.visitor.IVisitor;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class DeclCodeGenerator implements IVisitor
{
	private FileCodegenContext context;

	public DeclCodeGenerator(FileCodegenContext context)
	{
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
		if(Types.isZeroSized(struct))
			return;

		context.write('%');
		context.write(qualifiedName(struct));
		context.write(" = type { ");

		List<SemaDeclStruct.Field> fields = struct.fieldsByIndex();

		if(!fields.isEmpty())
		{
			if(!Types.isZeroSized(fields.get(0).type))
				context.write(TypeCodeGenerator.generate(fields.get(0).type, context.platform()));

			for(int i = 1; i != fields.size(); ++i)
			{
				if(!Types.isZeroSized(fields.get(i).type))
				{
					context.write(", ");
					context.write(TypeCodeGenerator.generate(fields.get(i).type, context.platform()));
				}
			}
		}

		context.write(" }\n");
	}

	private void generateParam(SemaDeclFunction.Param param, boolean isExternal)
	{
		if(Types.isZeroSized(param.type))
			return;

		context.write(TypeCodeGenerator.generate(param.type, context.platform()));

		if(param.type instanceof SemaTypeNonNullablePointer)
			context.write(" nonnull");

		if(!isExternal)
		{
			context.write(" %p$");
			context.write(param.name);
		}
	}

	private void generateSignature(SemaDeclFunction function)
	{
		boolean isExternal = function instanceof SemaDeclExternFunction;

		if(isExternal)
			context.write("declare ");

		else
		{
			context.write("define ");

			if(function.exported)
			{
				switch(context.build().type)
				{
				case LIBRARY: context.write("dllexport "); break;
				case EXECUTABLE: break;
				default: throw new AssertionError("unreachable");
				}
			}

			else
				context.write("private ");
		}

		context.write(TypeCodeGenerator.generate(function.ret, context.platform()));
		context.write(" @");

		if(isExternal)
			context.write(((SemaDeclExternFunction)function).externName.or(function.name()));
		else
			context.write(FunctionNameMangler.mangle(function));

		context.write('(');

		if(!function.params.isEmpty())
		{
			SemaDeclFunction.Param first = function.params.get(0);
			generateParam(first, isExternal);

			for(int i = 1; i != function.params.size(); ++i)
			{
				context.write(", ");

				SemaDeclFunction.Param param = function.params.get(i);
				generateParam(param, isExternal);
			}
		}

		context.write(")\n");
	}

	private void generateFunctionBody(SemaDeclDefinedFunction function)
	{
		context.write("{\n");

		for(SemaDeclFunction.Param param : function.params)
		{
			if(Types.isZeroSized(param.type))
				continue;

			String typeString = TypeCodeGenerator.generate(param.type, context.platform());
			BigInteger alignment = Types.alignof(param.type, context.platform());
			context.writef("\t%%%s = alloca %s, align %s\n", param.name, typeString, alignment);
			context.writef("\tstore %s %%p$%s, %s* %%%s\n", typeString, param.name, typeString, param.name);
		}

		if(!function.params.isEmpty())
			context.write('\n');

		for(Map.Entry<String, SemaDeclDefinedFunction.Local> entry : function.localsByName.entrySet())
		{
			SemaType type = entry.getValue().type;

			if(Types.isZeroSized(type))
				continue;

			String llvmType = TypeCodeGenerator.generate(type, context.platform());
			BigInteger align = Types.alignof(type, context.platform());
			context.writef("\t%%%s = alloca %s, align %s\n", entry.getKey(), llvmType, align);
		}

		if(!function.locals.isEmpty())
			context.write('\n');

		FunctionCodegenContext fcontext = new FunctionCodegenContext();
		StmtCodeGenerator stmtCodeGen = new StmtCodeGenerator(context, fcontext);

		for(SemaStmt stmt : function.body)
			stmtCodeGen.generate(stmt);

		stmtCodeGen.finish(function);

		context.write("}\n");
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
		if(Types.isZeroSized(global.type))
			return;

		String qualifiedName = qualifiedName(global);
		String typeString = TypeCodeGenerator.generate(global.type, context.platform());
		String initializerString = global.init.map(i -> ExprCodeGenerator.generate(i, context, null).unwrap()).or("zeroinitializer");
		String kind = Types.isConst(global.type) ? "constant" : "global";
		context.writef("@%s = private %s %s %s\n", qualifiedName, kind, typeString, initializerString);
	}

	private void visit(SemaDeclTypeAlias alias)
	{}

	private void visit(SemaDeclOperator operator)
	{}
}
