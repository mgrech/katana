package katana.sema;

import katana.ast.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Program
{
	public Module findOrCreateModule(Path path)
	{
		Module parent = root;

		for(String component : path.components)
		{
			Module child = parent.children.get(component);

			if(child == null)
			{
				List<String> childComponents = new ArrayList<>();
				childComponents.addAll(parent.path.components);
				childComponents.add(component);
				Path childPath = new Path(childComponents);

				child = new Module(component, childPath, parent);
				parent.children.put(component, child);
			}

			parent = child;
		}

		return parent;
	}

	public Optional<Module> findModule(Path path)
	{
		Module current = root;

		for(String component : path.components)
		{
			if(!current.children.containsKey(component))
				return Optional.empty();

			current = current.children.get(component);
		}

		return Optional.of(current);
	}

	public Module root = new Module("", new Path(), null);
}
