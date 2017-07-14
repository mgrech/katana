// Copyright 2017 Markus Grech
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

package katana.scanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SourceFile
{
	private static class Line
	{
		public int number;
		public int length;

		public Line(int number, int length)
		{
			this.number = number;
			this.length = length;
		}
	}

	private final Path path;
	private final int[] codepoints;
	private final TreeMap<Integer, Line> linesByOffset;
	private final List<String> lines;

	private SourceFile(Path path, int[] codepoints, TreeMap<Integer, Line> linesByOffset, List<String> lines)
	{
		this.path = path;
		this.codepoints = codepoints;
		this.linesByOffset = linesByOffset;
		this.lines = lines;
	}

	public static SourceFile load(Path root, Path path) throws IOException
	{
		byte[] bytes = Files.readAllBytes(path);
		int[] codepoints = new String(bytes, StandardCharsets.UTF_8).codePoints().toArray();

		TreeMap<Integer, Line> linesByOffset = new TreeMap<>();

		int lineOffset = 0;
		int lineNumber = 0;

		for(int i = 0; i != codepoints.length; ++i)
			if(codepoints[i] == '\n')
			{
				linesByOffset.put(lineOffset, new Line(lineNumber, i - lineOffset));

				lineOffset = i + 1;
				++lineNumber;
			}

		List<String> lines = new ArrayList<>();

		int lineStart = 0;

		for(int i = 0; i != bytes.length; ++i)
			if(bytes[i] == '\n')
			{
				byte[] lineBytes = new byte[i - lineStart];
				System.arraycopy(bytes, lineStart, lineBytes, 0, lineBytes.length);
				lines.add(new String(lineBytes, StandardCharsets.UTF_8));
				lineStart = i + 1;
			}

		return new SourceFile(root.relativize(path), codepoints, linesByOffset, lines);
	}

	public Path path()
	{
		return path;
	}

	public int[] codepoints()
	{
		return codepoints;
	}

	public String line(int index)
	{
		return lines.get(index);
	}

	public SourceLocation resolve(int offset, int length)
	{
		Map.Entry<Integer, Line> lineEntry = linesByOffset.floorEntry(offset);
		int lineOffset = lineEntry.getKey();
		Line line = lineEntry.getValue();
		return new SourceLocation(this, line.number, offset - lineOffset, length, offset);
	}
}
