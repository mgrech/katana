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

import katana.backend.PlatformContext;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclImportedOverloadSet;

import java.util.IdentityHashMap;
import java.util.Map;

public class DeclDepSolver
{
	private PlatformContext context;
	private IdentityHashMap<SemaDecl, DepSolveState> states = new IdentityHashMap<>();
	private IdentityHashMap<SemaDecl, DeclInfo> infos = new IdentityHashMap<>();

	private DeclDepSolver(PlatformContext context)
	{
		this.context = context;
	}

	private void solve(SemaDecl decl)
	{
		if(decl instanceof SemaDeclImportedOverloadSet)
			decl = ((SemaDeclImportedOverloadSet)decl).set;

		DepSolveState state = states.get(decl);

		if(state == null)
		{
			states.put(decl, DepSolveState.ONGOING);
			DeclIfaceValidator.validate(decl, infos.get(decl), context, this::solve);
			states.put(decl, DepSolveState.FINISHED);
			return;
		}

		switch(state)
		{
		case ONGOING:
			throw new RuntimeException("cyclic dependency detected");

		case FINISHED:
			return;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	public static void solve(IdentityHashMap<SemaDecl, DeclInfo> decls, PlatformContext context)
	{
		DeclDepSolver solver = new DeclDepSolver(context);

		for(Map.Entry<SemaDecl, DeclInfo> entry : decls.entrySet())
			solver.infos.put(entry.getKey(), entry.getValue());

		for(SemaDecl decl : decls.keySet())
			solver.solve(decl);
	}
}
