package katana.project;

import katana.utils.Maybe;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Project
{
	public final Path root;
	public final String name;
	public final List<Path> katanaFiles = new ArrayList<>();
	public final List<Path> cFiles = new ArrayList<>();
	public final List<Path> cppFiles = new ArrayList<>();
	public final ProjectType type;
	public final Maybe<String> entryPoint;

	public Project(Path root, String name, ProjectType type, Maybe<String> entryPoint)
	{
		this.name = name;
		this.root = root;
		this.type = type;
		this.entryPoint = entryPoint;
	}
}
