// Copyright 2016-2017 Markus Grech
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
import katana.diag.CompileException;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclImportedOverloadSet;

import java.util.IdentityHashMap;
import java.util.Map;

public class DeclDepResolver
{
	private PlatformContext context;
	private IdentityHashMap<SemaDecl, DepResolveState> states = new IdentityHashMap<>();
	private IdentityHashMap<SemaDecl, DeclInfo> infos = new IdentityHashMap<>();

	private DeclDepResolver(PlatformContext context)
	{
		this.context = context;
	}

	private void process(SemaDecl decl)
	{
		if(decl instanceof SemaDeclImportedOverloadSet)
			decl = ((SemaDeclImportedOverloadSet)decl).set;

		DepResolveState state = states.get(decl);

		if(state == null)
		{
			states.put(decl, DepResolveState.ONGOING);
			DeclIfaceValidator.validate(decl, infos.get(decl), context, this::process);
			states.put(decl, DepResolveState.FINISHED);
			return;
		}

		switch(state)
		{
		case ONGOING:
			throw new CompileException("cyclic dependency detected");

		case FINISHED:
			return;

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	public static void process(IdentityHashMap<SemaDecl, DeclInfo> decls, PlatformContext context)
	{
		DeclDepResolver resolver = new DeclDepResolver(context);

		for(Map.Entry<SemaDecl, DeclInfo> entry : decls.entrySet())
			resolver.infos.put(entry.getKey(), entry.getValue());

		for(SemaDecl decl : decls.keySet())
			resolver.process(decl);
	}
}
