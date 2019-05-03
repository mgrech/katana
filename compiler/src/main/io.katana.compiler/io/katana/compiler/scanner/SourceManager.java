// Copyright 2017-2019 Markus Grech
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

package io.katana.compiler.scanner;

import io.katana.compiler.utils.Maybe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class SourceManager
{
	private final Map<Path, SourceFile> files;

	private SourceManager(Map<Path, SourceFile> files)
	{
		this.files = files;
	}

	public static SourceManager loadFiles(Set<Path> paths) throws IOException
	{
		var files = new TreeMap<Path, SourceFile>();

		for(var path : paths)
			files.put(path, SourceFile.load(path));

		return new SourceManager(files);
	}

	public Collection<SourceFile> files()
	{
		return files.values();
	}

	public Maybe<SourceFile> get(Path path)
	{
		return Maybe.wrap(files.get(path));
	}
}
