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

import io.katana.compiler.ast.decl.AstDeclFunctionDef;
import io.katana.compiler.ast.decl.AstDeclOverloadSet;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.sema.decl.SemaDecl;
import io.katana.compiler.sema.decl.SemaDeclFunctionDef;
import io.katana.compiler.sema.decl.SemaDeclOverloadSet;

import java.util.IdentityHashMap;

public class DeclImplValidator
{
	public static void validate(IdentityHashMap<SemaDecl, DeclInfo> decls, PlatformContext context)
	{
		for(var entry : decls.entrySet())
		{
			var decl = entry.getKey();

			if(decl instanceof SemaDeclOverloadSet)
			{
				var set = (SemaDeclOverloadSet)decl;
				var info = entry.getValue();
				validateOverloadSet(set, (AstDeclOverloadSet)info.astDecl, context);
			}
		}
	}

	private static void validateOverloadSet(SemaDeclOverloadSet semaSet, AstDeclOverloadSet set, PlatformContext context)
	{
		for(var i = 0; i != semaSet.overloads.size(); ++i)
		{
			var overload = semaSet.overloads.get(i);

			if(!(overload instanceof SemaDeclFunctionDef))
				continue;

			var semaFunction = (SemaDeclFunctionDef)overload;
			var function = (AstDeclFunctionDef)set.overloads.get(i);
			validateDefinedFunction(semaFunction, function, context);
		}
	}

	private static void validateDefinedFunction(SemaDeclFunctionDef semaDecl, AstDeclFunctionDef decl, PlatformContext context)
	{
		var validator = new StmtValidator(semaDecl, semaDecl.scope, context, (ign) -> {});

		for(var stmt : decl.body)
		{
			var semaStmt = validator.validate(stmt);
			semaDecl.add(semaStmt);
		}

		validator.validateGotoTargets();
	}
}
