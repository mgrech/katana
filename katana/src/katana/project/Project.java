package katana.project;

import katana.utils.Maybe;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Project
{
	public final Path root;
	public final List<Path> katanaFiles = new ArrayList<>();
	public final List<Path> cFiles = new ArrayList<>();
	public final Maybe<String> entryPoint;

	public Project(Path root, Maybe<String> entryPoint)
	{
		this.root = root;
		this.entryPoint = entryPoint;
	}
}
