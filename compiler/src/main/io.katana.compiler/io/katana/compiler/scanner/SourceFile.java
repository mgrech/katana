// Copyright 2017-2018 Markus Grech
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

	public static SourceFile load(Path path) throws IOException
	{
		var bytes = Files.readAllBytes(path);
		return fromBytes(path, bytes);
	}

	public static SourceFile fromBytes(Path path, byte[] bytes)
	{
		var codepoints = new String(bytes, StandardCharsets.UTF_8).codePoints().toArray();

		var linesByOffset = new TreeMap<Integer, Line>();

		var lineOffset = 0;
		var lineNumber = 0;

		for(var i = 0; i != codepoints.length; ++i)
			if(codepoints[i] == '\n')
			{
				linesByOffset.put(lineOffset, new Line(lineNumber, i - lineOffset));

				lineOffset = i + 1;
				++lineNumber;
			}

		if(lineOffset != codepoints.length)
			linesByOffset.put(lineNumber, new Line(lineNumber, codepoints.length - lineOffset));

		var lines = new ArrayList<String>();

		var lineStart = 0;

		for(var i = 0; i != bytes.length; ++i)
			if(bytes[i] == '\n')
			{
				var lineBytes = new byte[i - lineStart];
				System.arraycopy(bytes, lineStart, lineBytes, 0, lineBytes.length);
				lines.add(new String(lineBytes, StandardCharsets.UTF_8));
				lineStart = i + 1;
			}

		if(lineStart != bytes.length)
		{
			var lineBytes = new byte[bytes.length - lineStart];
			System.arraycopy(bytes, lineStart, lineBytes, 0, lineBytes.length);
			lines.add(new String(lineBytes, StandardCharsets.UTF_8));
		}

		return new SourceFile(path, codepoints, linesByOffset, lines);
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
		var lineEntry = linesByOffset.floorEntry(offset);
		var lineOffset = lineEntry.getKey();
		var line = lineEntry.getValue();
		return new SourceLocation(this, line.number, offset - lineOffset, length, offset);
	}

	public String slice(int offset, int length)
	{
		return new String(codepoints, offset, length);
	}
}
