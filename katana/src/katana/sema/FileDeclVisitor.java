package katana.sema;

import katana.ast.Path;
import katana.ast.decl.*;
import katana.ast.decl.Module;
import katana.ast.visitor.IVisitor;

import java.util.HashSet;
import java.util.Set;

public class FileDeclVisitor implements IVisitor
{
	public FileDeclVisitor(Program program)
	{
		this.program = program;
	}

	public void visit(Data data)
	{
		declsSeen = true;
		requireModule();
		currentModule.defineSymbol(data.name, data);
	}

	public void visit(Function function)
	{
		declsSeen = true;
		requireModule();
		currentModule.defineSymbol(function.name, function);
	}

	public void visit(Global global)
	{
		declsSeen = true;
		requireModule();
		currentModule.defineSymbol(global.name, global);
	}

	public void visit(Import import_)
	{
		if(declsSeen)
			throw new RuntimeException("imports must go before other decls");

		if(!imports.add(import_.path))
			throw new RuntimeException("duplicate import");
	}

	public void visit(Module module_)
	{
		declsSeen = true;
		currentModule = program.findOrCreateModule(module_.path);
	}

	public Set<Path> imports()
	{
		return imports;
	}

	private void requireModule()
	{
		if(currentModule == null)
			throw new RuntimeException("no module defined");
	}

	private Program program;
	private katana.sema.Module currentModule = null;
	private boolean declsSeen = false;
	private Set<Path> imports = new HashSet<>();
}
