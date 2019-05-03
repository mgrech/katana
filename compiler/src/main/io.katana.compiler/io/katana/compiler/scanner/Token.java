// Copyright 2016-2019 Markus Grech
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

public class Token
{
	public final int offset;
	public final int length;
	public final TokenCategory category;
	public final TokenType type;
	public final Object value;

	public Token(int offset, int length, TokenCategory category, TokenType type, Object value)
	{
		this.offset = offset;
		this.length = length;
		this.category = category;
		this.type = type;
		this.value = value;
	}

	public Token(TokenCategory category, TokenType type, Object value)
	{
		this(-1, -1, category, type, value);
	}

	public Token(TokenCategory category, TokenType type)
	{
		this(category, type, null);
	}

	public Token withSourceRange(int offset, int length)
	{
		return new Token(offset, length, this.category, this.type, this.value);
	}

	@Override
	public String toString()
	{
		return String.format("Token(offset=%s, length=%s, category=%s, type=%s, value=%s)",
		                     offset, length, category, type, value);
	}
}
