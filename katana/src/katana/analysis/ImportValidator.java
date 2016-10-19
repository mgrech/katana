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

package katana.analysis;

import katana.ast.AstFile;
import katana.ast.decl.AstDeclImport;
import katana.ast.decl.AstDeclRenamedImport;
import katana.sema.SemaModule;
import katana.sema.SemaProgram;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclImportedOverloadSet;
import katana.sema.decl.SemaDeclOverloadSet;
import katana.sema.decl.SemaDeclRenamedImport;
import katana.sema.scope.SemaScopeFile;
import katana.utils.Maybe;

import java.util.Arrays;

public class ImportValidator
{
	private static SemaModule checkModulePath(AstDeclImport import_, SemaProgram program)
	{
		Maybe<SemaModule> module = program.findModule(import_.path);

		if(module.isNone())
			throw new RuntimeException(String.format("import of unknown module '%s'", import_.path));

		return module.unwrap();
	}

	public static void validate(AstFile file, SemaScopeFile scope, SemaProgram program)
	{
		for(AstDeclImport import_ : file.imports.values())
		{
			SemaModule module = checkModulePath(import_, program);

			for(SemaDecl decl : module.decls().values())
				if(decl instanceof SemaDeclOverloadSet)
					scope.defineSymbol(new SemaDeclImportedOverloadSet((SemaDeclOverloadSet)decl));
				else if(decl.exported)
					scope.defineSymbol(decl);
		}

		for(AstDeclRenamedImport import_ : file.renamedImports.values())
		{
			SemaModule module = checkModulePath(import_, program);
			SemaDeclRenamedImport semaImport = new SemaDeclRenamedImport(module, import_.rename);

			for(SemaDecl decl : module.decls().values())
				if(decl instanceof SemaDeclOverloadSet)
					semaImport.decls.put(decl.name(), new SemaDeclImportedOverloadSet((SemaDeclOverloadSet)decl));
				else if(decl.exported)
					semaImport.decls.put(decl.name(), decl);

			scope.defineSymbol(semaImport);
		}
	}
}
