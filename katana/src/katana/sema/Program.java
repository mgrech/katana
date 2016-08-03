package katana.sema;

import katana.ast.Path;

import java.util.ArrayList;
import java.util.List;

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

	public Module root = new Module("", new Path(), null);
}
