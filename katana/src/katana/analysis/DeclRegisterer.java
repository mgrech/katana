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
import katana.ast.AstModule;
import katana.ast.decl.*;
import katana.sema.SemaModule;
import katana.sema.SemaProgram;
import katana.sema.decl.*;
import katana.sema.scope.SemaScopeFile;
import katana.visitor.IVisitor;

import java.util.IdentityHashMap;

@SuppressWarnings("unused")
public class DeclRegisterer implements IVisitor
{
	public static IdentityHashMap<SemaDecl, AstDecl> process(SemaProgram semaProgram, AstFile file, SemaScopeFile scope)
	{
		DeclRegisterer registerer = new DeclRegisterer();
		IdentityHashMap<SemaDecl, AstDecl> decls = new IdentityHashMap<>();

		for(AstModule module : file.modules.values())
		{
			SemaModule semaModule = semaProgram.findOrCreateModule(module.path);

			for(AstDecl decl : module.decls.values())
			{
				SemaDecl semaDecl = (SemaDecl)decl.accept(registerer, scope, semaModule);
				decls.put(semaDecl, decl);
			}
		}

		return decls;
	}

	private static void handleDecl(SemaDecl decl, SemaScopeFile scope, SemaModule module)
	{
		if(!module.declare(decl))
			throw new RuntimeException(String.format("redefinition of symbol '%s'", decl.qualifiedName()));

		scope.defineSymbol(decl);
	}

	public SemaDecl visit(AstDeclGlobal decl, SemaScopeFile scope, SemaModule module)
	{
		SemaDeclGlobal semaDecl = new SemaDeclGlobal(module, decl.exported, decl.opaque, decl.name);
		handleDecl(semaDecl, scope, module);
		return semaDecl;
	}

	public SemaDecl visit(AstDeclOverloadSet decl, SemaScopeFile scope, SemaModule module)
	{
		SemaDeclOverloadSet semaDecl = new SemaDeclOverloadSet(module, decl.name);
		handleDecl(semaDecl, scope, module);

		for(AstDeclFunction overload : decl.overloads)
		{
			SemaDeclFunction semaOverload;

			if(overload instanceof AstDeclExternFunction)
			{
				AstDeclExternFunction extOverload = (AstDeclExternFunction)overload;
				semaOverload = new SemaDeclExternFunction(module, overload.exported, overload.opaque, extOverload.externName, extOverload.name);
			}

			else if(overload instanceof AstDeclDefinedFunction)
			{
				AstDeclDefinedFunction defOverload = (AstDeclDefinedFunction)overload;
				semaOverload = new SemaDeclDefinedFunction(module, overload.exported, overload.opaque, defOverload.name);
			}

			else
				throw new AssertionError("unreachable");

			semaDecl.overloads.add(semaOverload);
		}

		return semaDecl;
	}

	public SemaDecl visit(AstDeclData decl, SemaScopeFile scope, SemaModule module)
	{
		SemaDeclData semaDecl = new SemaDeclData(module, decl.exported, decl.opaque, decl.name);
		handleDecl(semaDecl, scope, module);
		return semaDecl;
	}

	public SemaDecl visit(AstDeclTypeAlias decl, SemaScopeFile scope, SemaModule module)
	{
		SemaDeclTypeAlias semaDecl = new SemaDeclTypeAlias(module, decl.exported, decl.name);
		handleDecl(semaDecl, scope, module);
		return semaDecl;
	}
}
