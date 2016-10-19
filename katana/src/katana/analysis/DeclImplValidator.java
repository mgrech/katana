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

import katana.ast.decl.AstDeclDefinedFunction;
import katana.ast.decl.AstDeclOverloadSet;
import katana.ast.stmt.AstStmt;
import katana.backend.PlatformContext;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclDefinedFunction;
import katana.sema.decl.SemaDeclFunction;
import katana.sema.decl.SemaDeclOverloadSet;
import katana.sema.scope.SemaScopeFile;
import katana.sema.scope.SemaScopeFunction;
import katana.sema.stmt.SemaStmt;
import katana.visitor.IVisitor;

import java.util.IdentityHashMap;
import java.util.Map;

public class DeclImplValidator implements IVisitor
{
	public static void validate(IdentityHashMap<SemaDecl, DeclInfo> decls, PlatformContext context)
	{
		for(Map.Entry<SemaDecl, DeclInfo> entry : decls.entrySet())
		{
			SemaDecl decl = entry.getKey();

			if(decl instanceof SemaDeclOverloadSet)
			{
				SemaDeclOverloadSet set = (SemaDeclOverloadSet)decl;
				DeclInfo info = entry.getValue();
				validateOverloadSet(set, (AstDeclOverloadSet)info.astDecl, context);
			}
		}
	}

	private static void validateOverloadSet(SemaDeclOverloadSet semaSet, AstDeclOverloadSet set, PlatformContext context)
	{
		for(int i = 0; i != semaSet.overloads.size(); ++i)
		{
			SemaDeclFunction overload = semaSet.overloads.get(i);

			if(!(overload instanceof SemaDeclDefinedFunction))
				continue;

			SemaDeclDefinedFunction semaFunction = (SemaDeclDefinedFunction)overload;
			AstDeclDefinedFunction function = (AstDeclDefinedFunction)set.overloads.get(i);
			validateDefinedFunction(semaFunction, function, context);
		}
	}

	private static void validateDefinedFunction(SemaDeclDefinedFunction semaDecl, AstDeclDefinedFunction decl, PlatformContext context)
	{
		StmtValidator validator = new StmtValidator(semaDecl, semaDecl.scope, context, (ign) -> {});

		for(AstStmt stmt : decl.body)
		{
			SemaStmt semaStmt = validator.validate(stmt);
			semaDecl.add(semaStmt);
		}

		validator.validateGotoTargets();
	}
}
