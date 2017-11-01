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

package io.katana.compiler.scanner;

public class SourceLocation
{
	public final SourceFile file;
	public final int line;
	public final int column;
	public final int length;
	public final int offset;

	public SourceLocation(SourceFile file, int line, int column, int length, int offset)
	{
		this.file = file;
		this.line = line;
		this.column = column;
		this.length = length;
		this.offset = offset;
	}

	@Override
	public String toString()
	{
		int beginColumn = column + 1;
		int endColumn = beginColumn + length;
		return String.format("%s:%s:%s-%s", file.path(), line + 1, beginColumn, endColumn);
	}
}
