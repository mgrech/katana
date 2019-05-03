// Copyright 2016-2019 Markus Grech
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

import io.katana.compiler.BuiltinType;
import io.katana.compiler.Inlining;
import io.katana.compiler.analysis.Types;
import io.katana.compiler.ast.AstPath;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.backend.llvm.FileCodegenContext;
import io.katana.compiler.backend.llvm.ir.IrModule;
import io.katana.compiler.backend.llvm.ir.IrModuleBuilder;
import io.katana.compiler.backend.llvm.ir.decl.*;
import io.katana.compiler.backend.llvm.ir.type.IrType;
import io.katana.compiler.backend.llvm.ir.type.IrTypes;
import io.katana.compiler.backend.llvm.ir.value.IrValues;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.TypeString;
import io.katana.compiler.project.BuildTarget;
import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.SemaProgram;
import io.katana.compiler.sema.decl.SemaDecl;
import io.katana.compiler.sema.decl.SemaDeclFunction;
import io.katana.compiler.sema.decl.SemaDeclOverloadSet;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Maybe;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class ProgramCodegen
{
	private static void declareIntrinsic(IrModuleBuilder builder, IrType returnType, String name, IrType... parameterTypes)
	{
		var parameters = Arrays.stream(parameterTypes)
		                       .map(IrFunctionParameter::new)
		                       .collect(Collectors.toList());

		var signature = new IrFunctionSignature(returnType, name, parameters);
		builder.declareFunction(signature);
	}

	private static void generateIntrinsicDecls(IrModuleBuilder builder, PlatformContext platform)
	{
		for(var type : new IrType[]{IrTypes.I8, IrTypes.I16, IrTypes.I32, IrTypes.I64})
		{
			declareIntrinsic(builder, type, "llvm.fshl."  + type, type, type, type);
			declareIntrinsic(builder, type, "llvm.fshr."  + type, type, type, type);
			declareIntrinsic(builder, type, "llvm.ctpop." + type, type);
			declareIntrinsic(builder, type, "llvm.ctlz."  + type, type, IrTypes.I1);
			declareIntrinsic(builder, type, "llvm.cttz."  + type, type, IrTypes.I1);
		}

		for(var type : new IrType[]{IrTypes.I16, IrTypes.I32, IrTypes.I64})
			declareIntrinsic(builder, type, "llvm.bswap." + type, type);

		var byteptr = IrTypes.ofPointer(IrTypes.I8);
		var nint = TypeCodegen.generate(SemaTypeBuiltin.INT, platform);
		declareIntrinsic(builder, IrTypes.VOID, "llvm.memcpy.p0i8.p0i8."  + nint, byteptr, byteptr, nint, IrTypes.I1);
		declareIntrinsic(builder, IrTypes.VOID, "llvm.memmove.p0i8.p0i8." + nint, byteptr, byteptr, nint, IrTypes.I1);
		declareIntrinsic(builder, IrTypes.VOID, "llvm.memset.p0i8."       + nint, byteptr, IrTypes.I8, nint, IrTypes.I1);
	}

	private static void generateDecls(DeclCodegen codegen, SemaModule module)
	{
		for(var child : module.children().values())
			generateDecls(codegen, child);

		module.decls().values().forEach(codegen::generate);
	}

	private static IrDeclFunctionDef createMain(SemaDecl func, PlatformContext context)
	{
		var builder = new IrFunctionBuilder();

		var returnType = ((SemaDeclFunction)func).returnType;
		var returnTypeIr = TypeCodegen.generate(returnType, context);
		var function = IrValues.ofSymbol(func.qualifiedName().toString());
		var result = builder.call(returnTypeIr, function, Collections.emptyList(), Collections.emptyList(), Inlining.AUTO);

		if(result.isNone())
			builder.ret(IrTypes.I32, Maybe.some(IrValues.ofConstant(0)));
		else
			builder.ret(IrTypes.I32, result.map(v -> v));

		var signature = new IrFunctionSignature(Linkage.EXTERNAL, DllStorageClass.NONE, IrTypes.I32, "main", Collections.emptyList());
		return new IrDeclFunctionDef(signature, builder.build());
	}

	private static Maybe<SemaDecl> findDeclByPath(SemaProgram program, String pathString)
	{
		var path = AstPath.fromString(pathString);
		var last = path.components.size() - 1;
		var symbol = path.components.get(last);
		path.components.remove(last);

		var module = program.findModule(path);

		if(module.isNone())
			return Maybe.none();

		return module.unwrap().findDecl(symbol);
	}

	private static SemaDecl findEntryPoint(SemaProgram program, String name)
	{
		var entry = findDeclByPath(program, name);

		if(entry.isNone())
			throw new CompileException(String.format("entry point '%s' could not found", name));

		var decl = entry.unwrap();

		if(!(decl instanceof SemaDeclOverloadSet))
			throw new CompileException("the specified entry point symbol does not refer to function");

		var set = (SemaDeclOverloadSet)decl;

		if(set.overloads.size() != 1)
			throw new CompileException("entry point function may not be overloaded");

		var func = set.overloads.get(0);

		if(Types.isVoid(func.returnType) || Types.isBuiltin(func.returnType, BuiltinType.INT32))
			return func;

		throw new CompileException(String.format("entry point must return 'void' or 'int32', got '%s'", TypeString.of(func.returnType)));
	}

	public static IrModule generate(BuildTarget build, SemaProgram program, PlatformContext platform)
	{
		var builder = new IrModuleBuilder();
		var stringPool = new StringPool();
		var context = new FileCodegenContext(build, platform, stringPool);

		builder.declareTargetTriple(context.platform().target());
		generateIntrinsicDecls(builder, platform);
		generateDecls(new DeclCodegen(context, builder), program.rootModule);
		stringPool.generate(builder);

		if(build.entryPoint != null)
		{
			var entryPoint = findEntryPoint(program, build.entryPoint);
			var wrapper = createMain(entryPoint, platform);
			builder.append(wrapper);
		}

		return builder.build();
	}
}
