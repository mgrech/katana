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
import katana.ast.AstProgram;
import katana.ast.decl.AstDecl;
import katana.backend.PlatformContext;
import katana.sema.SemaProgram;
import katana.sema.decl.SemaDecl;
import katana.sema.scope.SemaScopeFile;

import java.util.IdentityHashMap;
import java.util.Map;

public class ProgramValidator
{
	public static SemaProgram validate(AstProgram program, PlatformContext context)
	{
		SemaProgram semaProgram = new SemaProgram();

		IdentityHashMap<AstFile, SemaScopeFile> files = new IdentityHashMap<>();
		IdentityHashMap<SemaDecl, DeclInfo> decls = new IdentityHashMap<>();

		for(AstFile file : program.files.values())
		{
			SemaScopeFile scope = new SemaScopeFile();
			files.put(file, scope);

			for(Map.Entry<SemaDecl, AstDecl> decl : DeclRegisterer.process(semaProgram, file, scope).entrySet())
				decls.put(decl.getKey(), new DeclInfo(decl.getValue(), scope));
		}

		for(Map.Entry<AstFile, SemaScopeFile> file : files.entrySet())
			ImportValidator.validate(file.getKey(), file.getValue(), semaProgram);

		DeclDepSolver.solve(decls, context);
		DeclImplValidator.validate(decls, context);

		return semaProgram;
	}
}
