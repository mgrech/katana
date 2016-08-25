package katana.sema;

import katana.ast.Path;
import katana.visitor.IVisitable;

public abstract class Decl implements IVisitable
{
	protected Decl(Module module)
	{
		this.module = module;
	}

	public abstract String name();

	public Path qualifiedName()
	{
		Path path = new Path();
		path.components.addAll(module.path().components);
		path.components.add(name());
		return path;
	}

	public Module module()
	{
		return module;
	}

	private Module module;
}
