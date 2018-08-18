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
import io.katana.compiler.backend.llvm.ir.*;
import io.katana.compiler.project.BuildType;
import io.katana.compiler.sema.decl.*;
import io.katana.compiler.sema.expr.SemaExpr;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class DeclCodeGenerator implements IVisitor
{
	private final FileCodegenContext context;
	private final IrModuleBuilder builder;

	public DeclCodeGenerator(FileCodegenContext context, IrModuleBuilder builder)
	{
		this.context = context;
		this.builder = builder;
	}

	public void lower(SemaDecl decl)
	{
		decl.accept(this);
	}

	private Maybe<IrValue> lower(SemaExpr expr)
	{
		return ExprCodeGenerator.generate(expr, context, null);
	}

	private IrType lower(SemaType type)
	{
		return TypeCodeGenerator.generate(type, context.platform());
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

		var name = qualifiedName(struct);

		var fields = struct.fieldsByIndex().stream()
		                                   .map(f -> f.type)
		                                   .filter(t -> !Types.isZeroSized(t))
		                                   .map(this::lower)
		                                   .collect(Collectors.toList());

		builder.defineType(name, fields);
	}

	private IrFunctionParameter lower(SemaDeclFunction.Param parameter, boolean external)
	{
		var type = lower(parameter.type);
		var nonnull = Types.isNonNullablePointer(parameter.type);
		var name = external ? parameter.name : "p$" + parameter.name;
		return new IrFunctionParameter(type, name, nonnull);
	}

	private List<IrInstr> lowerBody(SemaDeclDefinedFunction function)
	{
		var builder = new IrFunctionBuilder();

		for(var param : function.params)
		{
			if(Types.isZeroSized(param.type))
				continue;

			var type = lower(param.type);
			var alignment = Types.alignof(param.type, context.platform());
			var paramIr = IrValues.ofSsa("p$" + param.name);
			var paramCopy = IrValues.ofSsa(param.name);
			builder.alloca(paramCopy, type, alignment);
			builder.store(type, paramIr, paramCopy);
		}

		for(var entry : function.localsByName.entrySet())
		{
			var type = entry.getValue().type;

			if(Types.isZeroSized(type))
				continue;

			var typeIr = lower(type);
			var alignment = Types.alignof(type, context.platform());
			builder.alloca(IrValues.ofSsa(entry.getKey()), typeIr, alignment);
		}

		var stmtCodeGen = new StmtCodeGenerator(context, builder);

		for(var stmt : function.body)
			stmtCodeGen.generate(stmt);

		stmtCodeGen.finish(function);
		return builder.build();
	}

	private void lower(SemaDeclDefinedFunction function)
	{
		var linkage = function.exported ? Linkage.EXTERNAL : Linkage.PRIVATE;

		var dllStorageClass = context.build().type == BuildType.LIBRARY_SHARED
		                      ? function.exported ? DllStorageClass.DLLEXPORT : DllStorageClass.NONE
		                      : DllStorageClass.NONE;

		var returnType = lower(function.ret);
		var name = FunctionNameMangler.mangle(function);

		var params = function.params.stream()
		                            .filter(p -> !Types.isZeroSized(p.type))
		                            .map(p -> lower(p, false))
		                            .collect(Collectors.toList());

		var signature = new IrFunctionSignature(linkage, dllStorageClass, returnType, name, params);
		builder.defineFunction(signature, lowerBody(function));
	}

	private void lower(SemaDeclExternFunction function)
	{
		var returnType = lower(function.ret);
		var name = function.externName.or(function.name());

		var params = function.params.stream()
		                            .filter(p -> !Types.isZeroSized(p.type))
		                            .map(p -> lower(p, true))
		                            .collect(Collectors.toList());

		var signature = new IrFunctionSignature(Linkage.EXTERNAL, DllStorageClass.NONE, returnType, name, params);
		builder.declareFunction(signature);
	}

	private void visit(SemaDeclOverloadSet set)
	{
		for(var overload : set.overloads)
			if(overload instanceof SemaDeclExternFunction)
				lower((SemaDeclExternFunction)overload);
			else if(overload instanceof SemaDeclDefinedFunction)
				lower((SemaDeclDefinedFunction)overload);
			else
				throw new AssertionError("unreachable");
	}

	private void visit(SemaDeclGlobal global)
	{
		if(Types.isZeroSized(global.type))
			return;

		var name = qualifiedName(global);
		var type = lower(global.type);
		var initializer = global.init.map(i -> lower(i).unwrap()).or(new IrValueConstant("zeroinitializer"));
		builder.defineGlobal(name, AddressMergeability.NONE, Types.isConst(global.type), type, initializer);
	}

	private void visit(SemaDeclTypeAlias alias)
	{}

	private void visit(SemaDeclOperator operator)
	{}
}
