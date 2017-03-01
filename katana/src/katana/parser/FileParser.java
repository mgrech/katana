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

package katana.parser;

import katana.ast.AstFile;
import katana.ast.AstModule;
import katana.ast.AstPath;
import katana.ast.decl.*;
import katana.diag.CompileException;
import katana.diag.DiagnosticsManager;
import katana.op.Operator;
import katana.scanner.Scanner;
import katana.scanner.SourceFile;
import katana.scanner.TokenType;
import katana.visitor.IVisitor;

@SuppressWarnings("unused")
public class FileParser implements IVisitor
{
	private AstFile file;
	private AstModule module = null;

	private FileParser(AstFile file)
	{
		this.file = file;
	}

	public static AstFile parse(SourceFile file, DiagnosticsManager diag)
	{
		Scanner scanner = new Scanner(file);
		scanner.advance();
		ParseContext ctx = new ParseContext(scanner, diag);

		AstFile ast = new AstFile();
		FileParser parser = new FileParser(ast);

		while(ctx.token().type != TokenType.END)
		{
			AstDecl decl = DeclParser.parse(ctx);
			decl.accept(parser);
		}

		ast.lateParseExprs = ctx.lateParseExprs();

		return ast;
	}

	private AstModule findOrCreateModule(AstPath path)
	{
		AstModule module = file.modules.get(path);

		if(module != null)
			return module;

		module = new AstModule(path);
		file.modules.put(path, module);
		return module;
	}

	private void visit(AstDeclImport decl)
	{
		if(module != null)
			throw new CompileException("imports must go first in a file");

		if(file.imports.get(decl.path) != null)
			throw new CompileException(String.format("duplicate import of module '%s'", decl.path));

		file.imports.put(decl.path, decl);
	}

	private void visit(AstDeclRenamedImport decl)
	{
		if(module != null)
			throw new CompileException("imports must go first in a file");

		if(file.renamedImports.get(decl.rename) != null)
			throw new CompileException(String.format("duplicate renamed import of module '%s' as '%s'", decl.path, decl.rename));

		file.renamedImports.put(decl.rename, decl);
	}

	private void visit(AstDeclModule decl)
	{
		module = findOrCreateModule(decl.path);
	}

	private void redefinitionError(String name)
	{
		throw new CompileException(String.format("redefinition of symbol '%s'", name));
	}

	private void requireModule()
	{
		if(module == null)
			throw new CompileException("module definition required");
	}

	private void handleDecl(AstDecl decl, String name)
	{
		requireModule();

		if(module.decls.get(name) != null)
			redefinitionError(name);

		module.decls.put(name, decl);
	}

	private void visit(AstDeclStruct decl)
	{
		handleDecl(decl, decl.name);
	}

	private void visit(AstDeclGlobal decl)
	{
		handleDecl(decl, decl.name);
	}

	private void visit(AstDeclTypeAlias decl)
	{
		handleDecl(decl, decl.name);
	}

	private AstDeclOverloadSet findOrCreateOverloadSet(String name)
	{
		requireModule();

		AstDecl decl = module.decls.get(name);

		if(decl != null)
		{
			if(decl instanceof AstDeclOverloadSet)
				return (AstDeclOverloadSet)decl;

			redefinitionError(name);
		}

		AstDeclOverloadSet set = new AstDeclOverloadSet(name);
		module.decls.put(name, set);
		return set;
	}

	private void visit(AstDeclDefinedFunction decl)
	{
		requireModule();
		AstDeclOverloadSet set = findOrCreateOverloadSet(decl.name);
		set.overloads.add(decl);
	}

	private void visit(AstDeclExternFunction decl)
	{
		requireModule();
		AstDeclOverloadSet set = findOrCreateOverloadSet(decl.name);
		set.overloads.add(decl);
	}

	private void visit(AstDeclOperator decl)
	{
		handleDecl(decl, Operator.declName(decl.operator.symbol, decl.operator.kind));
	}
}
