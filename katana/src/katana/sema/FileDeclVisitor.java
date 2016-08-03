package katana.sema;

import katana.ast.decl.*;
import katana.ast.decl.Module;
import katana.ast.visitor.IVisitor;

import java.util.ArrayList;
import java.util.List;

public class FileDeclVisitor implements IVisitor
{
	public FileDeclVisitor(Program program)
	{
		this.program = program;
	}

	public void visit(Data data)
	{
		onDecl();
		currentModule.defineSymbol(data.name, data);
	}

	public void visit(Function function)
	{
		onDecl();
		currentModule.defineSymbol(function.name, function);
	}

	public void visit(Global global)
	{
		onDecl();
		currentModule.defineSymbol(global.name, global);
	}

	public void visit(Import import_)
	{
		if(declsSeen)
			throw new RuntimeException("imports must go before other decls");

		imports.add(import_);
	}

	public void visit(Module module_)
	{
		currentModule = program.findOrCreateModule(module_.path);
	}

	public List<Import> imports()
	{
		return imports;
	}

	private void onDecl()
	{
		declsSeen = true;

		if(currentModule == null)
			throw new RuntimeException("no module defined");
	}

	private Program program;
	private katana.sema.Module currentModule = null;
	private boolean declsSeen = false;
	private List<Import> imports = new ArrayList<>();
}
