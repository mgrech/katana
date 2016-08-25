package katana.sema;

import katana.Maybe;
import katana.ast.Path;

public class Program
{
	public Module findOrCreateModule(Path path)
	{
		Module parent = root;

		for(String component : path.components)
			parent = parent.findOrCreateChild(component);

		return parent;
	}

	public Maybe<Module> findModule(Path path)
	{
		Module current = root;

		for(String component : path.components)
		{
			Maybe<Module> child = current.findChild(component);

			if(child.isNone())
				return child;

			current = child.get();
		}

		return Maybe.some(current);
	}

	public Module root = new Module("", new Path(), null);
}
