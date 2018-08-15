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
import io.katana.compiler.backend.llvm.ir.IrValueConstant;
import io.katana.compiler.sema.decl.*;
import io.katana.compiler.sema.type.SemaTypeNonNullablePointer;
import io.katana.compiler.visitor.IVisitor;

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
		var modulePath = decl.module().path();
		return modulePath.toString() + "." + decl.name();
	}

	private void visit(SemaDeclStruct struct)
	{
		if(Types.isZeroSized(struct))
			return;

		context.write('%');
		context.write(qualifiedName(struct));
		context.write(" = type { ");

		var fields = struct.fieldsByIndex();

		if(!fields.isEmpty())
		{
			if(!Types.isZeroSized(fields.get(0).type))
				context.write(TypeCodeGenerator.generate(fields.get(0).type, context.platform()));

			for(var i = 1; i != fields.size(); ++i)
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
		var isExternal = function instanceof SemaDeclExternFunction;

		if(isExternal)
			context.write("declare ");
		else
		{
			context.write("define ");

			if(function.exported)
			{
				switch(context.build().type)
				{
				case LIBRARY_SHARED: context.write("dllexport "); break;
				case LIBRARY_STATIC: break;
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
			var first = function.params.get(0);
			generateParam(first, isExternal);

			for(var i = 1; i != function.params.size(); ++i)
			{
				context.write(", ");

				var param = function.params.get(i);
				generateParam(param, isExternal);
			}
		}

		context.write(")\n");
	}

	private void generateFunctionBody(SemaDeclDefinedFunction function)
	{
		context.write("{\n");

		for(var param : function.params)
		{
			if(Types.isZeroSized(param.type))
				continue;

			var typeString = TypeCodeGenerator.generate(param.type, context.platform());
			var alignment = Types.alignof(param.type, context.platform());
			context.writef("\t%%%s = alloca %s, align %s\n", param.name, typeString, alignment);
			context.writef("\tstore %s %%p$%s, %s* %%%s\n", typeString, param.name, typeString, param.name);
		}

		if(!function.params.isEmpty())
			context.write('\n');

		for(var entry : function.localsByName.entrySet())
		{
			var type = entry.getValue().type;

			if(Types.isZeroSized(type))
				continue;

			var llvmType = TypeCodeGenerator.generate(type, context.platform());
			var align = Types.alignof(type, context.platform());
			context.writef("\t%%%s = alloca %s, align %s\n", entry.getKey(), llvmType, align);
		}

		if(!function.locals.isEmpty())
			context.write('\n');

		var fcontext = new FunctionCodegenContext(context);
		var stmtCodeGen = new StmtCodeGenerator(fcontext);

		for(var stmt : function.body)
			stmtCodeGen.generate(stmt);

		stmtCodeGen.finish(function);

		context.write("}\n");
	}

	private void visit(SemaDeclOverloadSet set)
	{
		for(var overload : set.overloads)
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

		var qualifiedName = qualifiedName(global);
		var typeString = TypeCodeGenerator.generate(global.type, context.platform());
		var initializerString = global.init.map(i -> ExprCodeGenerator.generate(i, null).unwrap()).or(new IrValueConstant("zeroinitializer"));
		var kind = Types.isConst(global.type) ? "constant" : "global";
		context.writef("@%s = private %s %s %s\n", qualifiedName, kind, typeString, initializerString);
	}

	private void visit(SemaDeclTypeAlias alias)
	{}

	private void visit(SemaDeclOperator operator)
	{}
}
