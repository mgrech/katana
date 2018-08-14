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
import io.katana.compiler.ast.decl.*;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.op.Operator;
import io.katana.compiler.sema.SemaModule;
import io.katana.compiler.sema.SemaProgram;
import io.katana.compiler.sema.decl.*;
import io.katana.compiler.sema.scope.SemaScopeFile;
import io.katana.compiler.visitor.IVisitor;

import java.util.IdentityHashMap;

@SuppressWarnings("unused")
public class DeclRegisterer implements IVisitor
{
	public static IdentityHashMap<SemaDecl, AstDecl> process(SemaProgram semaProgram, AstFile file, SemaScopeFile scope)
	{
		var registerer = new DeclRegisterer();
		var decls = new IdentityHashMap<SemaDecl, AstDecl>();

		for(var module : file.modules.values())
		{
			var semaModule = semaProgram.findOrCreateModule(module.path);

			for(var decl : module.decls.values())
			{
				if(!(decl instanceof AstDeclOperator))
				{
					var semaDecl = (SemaDecl)decl.accept(registerer, scope, semaModule);
					decls.put(semaDecl, decl);
				}
			}
		}

		return decls;
	}

	private static void handleDecl(SemaDecl decl, SemaScopeFile scope, SemaModule module)
	{
		if(!module.declare(decl))
			throw new CompileException(String.format("redefinition of symbol '%s'", decl.qualifiedName()));

		scope.defineSymbol(decl);
	}

	public SemaDecl visit(AstDeclGlobal decl, SemaScopeFile scope, SemaModule module)
	{
		var semaDecl = new SemaDeclGlobal(module, decl.exported, decl.opaque, decl.name);
		handleDecl(semaDecl, scope, module);
		return semaDecl;
	}

	public SemaDecl visit(AstDeclOverloadSet decl, SemaScopeFile scope, SemaModule module)
	{
		var semaDecl = new SemaDeclOverloadSet(module, decl.name);
		handleDecl(semaDecl, scope, module);

		for(var overload : decl.overloads)
		{
			SemaDeclFunction semaOverload;

			if(overload instanceof AstDeclExternFunction)
			{
				AstDeclExternFunction extOverload = (AstDeclExternFunction)overload;
				semaOverload = new SemaDeclExternFunction(module, overload.exported, overload.opaque, extOverload.externName, extOverload.name);
			}
			else if(overload instanceof AstDeclDefinedOperator)
			{
				var defOverload = (AstDeclDefinedOperator)overload;
				var opCandidates = scope.find(Operator.declName(defOverload.op, defOverload.kind));

				if(opCandidates.isEmpty())
				{
					var kind = defOverload.kind.toString().toLowerCase();
					var op = defOverload.op;
					throw new CompileException(String.format("no operator declaration found for '%s %s'", kind, op));
				}

				if(opCandidates.size() > 1)
				{
					var kind = defOverload.kind.toString().toLowerCase();
					var op = defOverload.op;
					throw new CompileException(String.format("multiple operator declarations found for '%s %s'", kind, op));
				}

				semaOverload = new SemaDeclDefinedOperator(module, overload.exported, overload.opaque, (SemaDeclOperator)opCandidates.get(0));
			}
			else if(overload instanceof AstDeclDefinedFunction)
			{
				var defOverload = (AstDeclDefinedFunction)overload;
				semaOverload = new SemaDeclDefinedFunction(module, overload.exported, overload.opaque, defOverload.name);
			}
			else
				throw new AssertionError("unreachable");

			semaDecl.overloads.add(semaOverload);
		}

		return semaDecl;
	}

	public SemaDecl visit(AstDeclStruct decl, SemaScopeFile scope, SemaModule module)
	{
		var semaDecl = new SemaDeclStruct(module, decl.exported, decl.opaque, decl.name, decl.abiCompat);
		handleDecl(semaDecl, scope, module);
		return semaDecl;
	}

	public SemaDecl visit(AstDeclTypeAlias decl, SemaScopeFile scope, SemaModule module)
	{
		var semaDecl = new SemaDeclTypeAlias(module, decl.exported, decl.name);
		handleDecl(semaDecl, scope, module);
		return semaDecl;
	}
}
