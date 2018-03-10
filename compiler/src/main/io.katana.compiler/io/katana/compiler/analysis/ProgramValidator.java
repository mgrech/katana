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

package io.katana.compiler.analysis;

import io.katana.compiler.ast.AstFile;
import io.katana.compiler.ast.AstModule;
import io.katana.compiler.ast.AstPath;
import io.katana.compiler.ast.AstProgram;
import io.katana.compiler.ast.decl.AstDecl;
import io.katana.compiler.ast.decl.AstDeclImport;
import io.katana.compiler.ast.decl.AstDeclOperator;
import io.katana.compiler.ast.decl.AstDeclRenamedImport;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.op.BuiltinOps;
import io.katana.compiler.op.OperatorParser;
import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.SemaProgram;
import io.katana.compiler.sema.decl.*;
import io.katana.compiler.sema.scope.SemaScopeFile;
import io.katana.compiler.utils.Maybe;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class ProgramValidator
{
	private static Map<AstPath, Map<String, SemaDeclOperator>> registerOperatorDecls(SemaProgram program, Map<AstFile, SemaScopeFile> scopesByFile)
	{
		Map<AstPath, Map<String, SemaDeclOperator>> result = new HashMap<>();

		for(Map.Entry<AstFile, SemaScopeFile> file : scopesByFile.entrySet())
			for(Map.Entry<AstPath, AstModule> module : file.getKey().modules.entrySet())
				for(AstDecl decl : module.getValue().decls.values())
					if(decl instanceof AstDeclOperator)
					{
						AstPath path = module.getKey();
						AstDeclOperator opDecl = (AstDeclOperator)decl;

						Map<String, SemaDeclOperator> map = result.get(path);

						if(map == null)
						{
							map = new HashMap<>();
							result.put(path, map);
						}

						SemaModule semaModule = program.findOrCreateModule(path);
						SemaDeclOperator semaDecl = new SemaDeclOperator(semaModule, opDecl.exported, opDecl.operator);

						if(!semaModule.declare(semaDecl))
						{
							String op = semaDecl.operator.symbol;
							String kind = semaDecl.operator.kind.toString().toLowerCase();
							String fmt = "redefinition of operator '%s %s'";
							throw new CompileException(String.format(fmt, op, kind));
						}

						file.getValue().defineSymbol(semaDecl);

						if(semaDecl.exported)
							map.put(semaDecl.name(), semaDecl);
					}

		return result;
	}

	private static void propagateOperatorDecls(Map<AstFile, SemaScopeFile> files, Map<AstPath, Map<String, SemaDeclOperator>> operators)
	{
		for(Map.Entry<AstFile, SemaScopeFile> entry : files.entrySet())
		{
			AstFile file = entry.getKey();
			SemaScopeFile scope = entry.getValue();

			for(AstPath path : file.imports.keySet())
			{
				Map<String, SemaDeclOperator> importedOps = operators.get(path);

				if(importedOps != null)
					for(SemaDeclOperator operator : importedOps.values())
						scope.defineSymbol(operator);
			}
		}
	}

	private static IdentityHashMap<AstFile, SemaScopeFile> createScopes(Collection<AstFile> files)
	{
		IdentityHashMap<AstFile, SemaScopeFile> result = new IdentityHashMap<>();

		for(AstFile file : files)
			result.put(file, new SemaScopeFile());

		return result;
	}

	private static IdentityHashMap<SemaDecl, DeclInfo> registerDecls(SemaProgram program, IdentityHashMap<AstFile, SemaScopeFile> scopes)
	{
		IdentityHashMap<SemaDecl, DeclInfo> result = new IdentityHashMap<>();

		for(Map.Entry<AstFile, SemaScopeFile> scope : scopes.entrySet())
			for(Map.Entry<SemaDecl, AstDecl> decl : DeclRegisterer.process(program, scope.getKey(), scope.getValue()).entrySet())
				result.put(decl.getKey(), new DeclInfo(decl.getValue(), scope.getValue()));

		return result;
	}

	private static SemaModule checkImportedPath(AstDeclImport import_, SemaProgram program)
	{
		Maybe<SemaModule> module = program.findModule(import_.path);

		if(module.isNone())
			throw new CompileException(String.format("import of unknown module '%s'", import_.path));

		return module.unwrap();
	}

	private static void validateFileImports(AstFile file, SemaScopeFile scope, SemaProgram program)
	{
		for(AstDeclImport import_ : file.imports.values())
		{
			SemaModule module = checkImportedPath(import_, program);

			for(SemaDecl decl : module.decls().values())
				if(decl instanceof SemaDeclOverloadSet)
					scope.defineSymbol(new SemaDeclImportedOverloadSet((SemaDeclOverloadSet)decl));
				else if(decl.exported && !(decl instanceof SemaDeclOperator))
					scope.defineSymbol(decl);
		}

		for(AstDeclRenamedImport import_ : file.renamedImports.values())
		{
			SemaModule module = checkImportedPath(import_, program);
			SemaDeclRenamedImport semaImport = new SemaDeclRenamedImport(module, import_.rename);

			for(SemaDecl decl : module.decls().values())
				if(decl instanceof SemaDeclOverloadSet)
					semaImport.decls.put(decl.name(), new SemaDeclImportedOverloadSet((SemaDeclOverloadSet)decl));
				else if(decl.exported)
					semaImport.decls.put(decl.name(), decl);

			scope.defineSymbol(semaImport);
		}
	}

	private static void validateImports(SemaProgram program, IdentityHashMap<AstFile, SemaScopeFile> scopes)
	{
		for(Map.Entry<AstFile, SemaScopeFile> scope : scopes.entrySet())
			validateFileImports(scope.getKey(), scope.getValue(), program);
	}

	private static void parseOperators(IdentityHashMap<AstFile, SemaScopeFile> scopes)
	{
		for(Map.Entry<AstFile, SemaScopeFile> scope : scopes.entrySet())
			OperatorParser.replace(scope.getKey().lateParseExprs, scope.getValue());
	}

	private static void registerBuiltinOps(Collection<SemaScopeFile> scopes)
	{
		for(SemaScopeFile scope : scopes)
		{
			for(SemaDeclOperator decl : BuiltinOps.PREFIX_OPS.values())
				scope.defineSymbol(decl);

			for(SemaDeclOperator decl : BuiltinOps.INFIX_OPS.values())
				scope.defineSymbol(decl);

			for(SemaDeclOperator decl : BuiltinOps.POSTFIX_OPS.values())
				scope.defineSymbol(decl);
		}
	}

	public static SemaProgram validate(AstProgram program, PlatformContext context)
	{
		SemaProgram semaProgram = new SemaProgram();

		IdentityHashMap<AstFile, SemaScopeFile> scopes = createScopes(program.files.values());
		Map<AstPath, Map<String, SemaDeclOperator>> operators = registerOperatorDecls(semaProgram, scopes);
		propagateOperatorDecls(scopes, operators);

		IdentityHashMap<SemaDecl, DeclInfo> decls = registerDecls(semaProgram, scopes);
		validateImports(semaProgram, scopes);
		registerBuiltinOps(scopes.values());
		parseOperators(scopes);

		DeclDepResolver.process(decls, context);
		DeclImplValidator.validate(decls, context);

		return semaProgram;
	}
}
