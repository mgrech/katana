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

package katana.project;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import katana.utils.ResourceExtractor;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectManager
{
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
	                                                  .serializeSpecialFloatingPointValues()
	                                                  .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
	                                                  .setLenient()
	                                                  .setPrettyPrinting()
	                                                  .create();

	private static final String PROJECT_CONFIG_NAME = "project.json";

	private static void validatePath(String pathString, Path root)
	{
		Path path = Paths.get(pathString);

		if(path.isAbsolute())
			throw new InvalidPathException(pathString, "given source path is absolute");

		if(!root.resolve(path).toAbsolutePath().normalize().startsWith(root))
			throw new InvalidPathException(path.toString(), "given source path is not a child of the project root");
	}

	public static ProjectConfig load(Path root) throws IOException
	{
		byte[] bytes = Files.readAllBytes(root.resolve(PROJECT_CONFIG_NAME));
		ProjectConfig project = GSON.fromJson(new StringReader(new String(bytes, StandardCharsets.UTF_8)), ProjectConfig.class);

		if(project == null)
			throw new JsonSyntaxException(String.format("%s is empty", PROJECT_CONFIG_NAME));

		for(String sourcePath : project.sourcePaths)
			validatePath(sourcePath, root);

		return project;
	}

	public static void createDefaultProject(Path path) throws IOException
	{
		ResourceExtractor.extract("project-template", path);
	}
}
