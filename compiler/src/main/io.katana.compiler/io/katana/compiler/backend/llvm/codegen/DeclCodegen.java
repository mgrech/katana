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

package io.katana.compiler.backend.llvm.codegen;

import io.katana.compiler.ExportKind;
import io.katana.compiler.analysis.Types;
import io.katana.compiler.backend.FunctionNameMangling;
import io.katana.compiler.backend.llvm.FileCodegenContext;
import io.katana.compiler.backend.llvm.ir.IrModuleBuilder;
import io.katana.compiler.backend.llvm.ir.decl.*;
import io.katana.compiler.backend.llvm.ir.instr.IrInstr;
import io.katana.compiler.backend.llvm.ir.type.IrType;
import io.katana.compiler.backend.llvm.ir.value.IrValue;
import io.katana.compiler.backend.llvm.ir.value.IrValueConstant;
import io.katana.compiler.backend.llvm.ir.value.IrValues;
import io.katana.compiler.project.BuildType;
import io.katana.compiler.sema.decl.*;
import io.katana.compiler.sema.expr.SemaExpr;
import io.katana.compiler.sema.type.SemaType;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class DeclCodegen implements IVisitor
{
	private final FileCodegenContext context;
	private final IrModuleBuilder builder;

	public DeclCodegen(FileCodegenContext context, IrModuleBuilder builder)
	{
		this.context = context;
		this.builder = builder;
	}

	public void generate(SemaDecl decl)
	{
		decl.accept(this);
	}

	private Maybe<IrValue> generate(SemaExpr expr)
	{
		return ExprCodegen.generate(expr, context, null);
	}

	private IrType generate(SemaType type)
	{
		return TypeCodegen.generate(type, context.platform());
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
		                                   .map(this::generate)
		                                   .collect(Collectors.toList());

		builder.defineType(name, fields);
	}

	private IrFunctionParameter generate(SemaDeclFunction.Param parameter, boolean external)
	{
		var type = generate(parameter.type);
		var nonnull = Types.isNonNullablePointer(parameter.type);
		var name = external ? parameter.name : "p$" + parameter.name;
		return new IrFunctionParameter(type, name, nonnull);
	}

	private List<IrInstr> generateBody(SemaDeclDefinedFunction function)
	{
		var builder = new IrFunctionBuilder();

		for(var param : function.params)
		{
			if(Types.isZeroSized(param.type))
				continue;

			var type = generate(param.type);
			var alignment = Types.alignof(param.type, context.platform());
			var paramIr = IrValues.ofSsa("p$" + param.name);
			var paramCopy = IrValues.ofSsa(param.name);
			builder.alloca(paramCopy, type, alignment);
			builder.store(type, paramIr, paramCopy);
		}

		for(var entry : function.variablesByName.entrySet())
		{
			var type = entry.getValue().type;

			if(Types.isZeroSized(type))
				continue;

			var typeIr = generate(type);
			var alignment = Types.alignof(type, context.platform());
			builder.alloca(IrValues.ofSsa(entry.getKey()), typeIr, alignment);
		}

		var stmtLowerer = new StmtCodegen(context, builder);

		for(var stmt : function.body)
			stmtLowerer.generate(stmt);

		stmtLowerer.finish(function);
		return builder.build();
	}

	private void generate(SemaDeclDefinedFunction function)
	{
		var exported = function.exportKind != ExportKind.HIDDEN;
		var linkage = exported ? Linkage.EXTERNAL : Linkage.PRIVATE;

		var dllStorageClass = context.build().type == BuildType.LIBRARY_SHARED
		                      ? exported ? DllStorageClass.DLLEXPORT : DllStorageClass.NONE
		                      : DllStorageClass.NONE;

		var returnType = generate(function.ret);
		var name = FunctionNameMangling.of(function);

		var params = function.params.stream()
		                            .filter(p -> !Types.isZeroSized(p.type))
		                            .map(p -> generate(p, false))
		                            .collect(Collectors.toList());

		var signature = new IrFunctionSignature(linkage, dllStorageClass, returnType, name, params);
		builder.defineFunction(signature, generateBody(function));
	}

	private void generate(SemaDeclExternFunction function)
	{
		var returnType = generate(function.ret);
		var name = function.externName.or(function.name());

		var params = function.params.stream()
		                            .filter(p -> !Types.isZeroSized(p.type))
		                            .map(p -> generate(p, true))
		                            .collect(Collectors.toList());

		var signature = new IrFunctionSignature(Linkage.EXTERNAL, DllStorageClass.NONE, returnType, name, params);
		builder.declareFunction(signature);
	}

	private void visit(SemaDeclOverloadSet set)
	{
		for(var overload : set.overloads)
			if(overload instanceof SemaDeclExternFunction)
				generate((SemaDeclExternFunction)overload);
			else if(overload instanceof SemaDeclDefinedFunction)
				generate((SemaDeclDefinedFunction)overload);
			else
				throw new AssertionError("unreachable");
	}

	private void visit(SemaDeclGlobal global)
	{
		if(Types.isZeroSized(global.type))
			return;

		var name = qualifiedName(global);
		var type = generate(global.type);
		var initializer = global.init.map(i -> generate(i).unwrap()).or(new IrValueConstant("zeroinitializer"));
		builder.defineGlobal(name, AddressMergeability.NONE, Types.isConst(global.type), type, initializer);
	}

	private void visit(SemaDeclTypeAlias alias)
	{}

	private void visit(SemaDeclOperator operator)
	{}
}
