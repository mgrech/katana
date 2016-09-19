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

package katana.sema;

import katana.ast.File;
import katana.backend.PlatformContext;
import katana.parser.FileParser;
import katana.sema.decl.Decl;
import katana.utils.Maybe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class ProgramValidator
{
	private IdentityHashMap<Decl, FileValidator> validatorsByDecl = new IdentityHashMap<>();
	private IdentityHashMap<Decl, ValidationState> statesByDecl = new IdentityHashMap<>();

	private void doValidate(Decl decl)
	{
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

	private Program doValidate(List<Path> filePaths, PlatformContext context) throws IOException
	{
		Program program = new Program();

		List<FileValidator> validators = new ArrayList<>();

		for(Path filePath : filePaths)
		{
			File file = FileParser.parse(filePath);
			FileValidator validator = new FileValidator(context, program, this::doValidate);
			validators.add(validator);

			for(katana.ast.decl.Decl decl : file.decls)
			{
				Maybe<Decl> semaDecl = validator.register(decl);

				if(semaDecl.isSome())
					validatorsByDecl.put(semaDecl.unwrap(), validator);
			}
		}

		for(FileValidator validator : validators)
			validator.validateImports();

		for(Decl decl : validatorsByDecl.keySet())
			doValidate(decl);

		for(FileValidator validator : validators)
			validator.finish();

		return program;
	}

	public static Program validate(List<Path> filePaths, PlatformContext context) throws IOException
	{
		return new ProgramValidator().doValidate(filePaths, context);
	}
}
