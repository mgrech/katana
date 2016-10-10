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
import katana.ast.decl.AstDecl;
import katana.backend.PlatformContext;
import katana.parser.FileParser;
import katana.sema.SemaProgram;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclImportedOverloadSet;
import katana.utils.Maybe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class ProgramValidator
{
	private IdentityHashMap<SemaDecl, FileValidator> validatorsByDecl = new IdentityHashMap<>();
	private IdentityHashMap<SemaDecl, ValidationState> statesByDecl = new IdentityHashMap<>();

	private void doValidate(SemaDecl decl)
	{
		if(decl instanceof SemaDeclImportedOverloadSet)
			decl = ((SemaDeclImportedOverloadSet)decl).set;

		ValidationState state = statesByDecl.get(decl);

		if(state == null)
		{
			statesByDecl.put(decl, ValidationState.ONGOING);
			FileValidator validator = validatorsByDecl.get(decl);
			validator.validate(decl);
			statesByDecl.put(decl, ValidationState.FINISHED);
		}

		else
		{
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
	}

	private SemaProgram doValidate(List<Path> filePaths, PlatformContext context) throws IOException
	{
		SemaProgram program = new SemaProgram();

		List<FileValidator> validators = new ArrayList<>();

		for(Path filePath : filePaths)
		{
			AstFile file = FileParser.parse(filePath);
			FileValidator validator = new FileValidator(context, program, this::doValidate);
			validators.add(validator);

			for(AstDecl decl : file.decls)
			{
				Maybe<SemaDecl> semaDecl = validator.register(decl);

				if(semaDecl.isSome())
					validatorsByDecl.put(semaDecl.unwrap(), validator);
			}
		}

		for(FileValidator validator : validators)
			validator.validateImports();

		for(SemaDecl decl : validatorsByDecl.keySet())
			doValidate(decl);

		for(FileValidator validator : validators)
			validator.finish();

		return program;
	}

	public static SemaProgram validate(List<Path> filePaths, PlatformContext context) throws IOException
	{
		return new ProgramValidator().doValidate(filePaths, context);
	}
}
