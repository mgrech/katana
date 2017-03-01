// Copyright 2016-2017 Markus Grech
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

public class Token
{
	public final TokenCategory category;
	public final TokenType type;
	public final int offset;
	public final String value;
	public final Object data;

	public Token(TokenCategory category, TokenType type, String value)
	{
		this(category, type, -1, value, null);
	}

	public Token(TokenCategory category, TokenType type, int offset, String value, Object data)
	{
		this.category = category;
		this.type = type;
		this.value = value;
		this.offset = offset;
		this.data = data;
	}

	public Token withOffset(int offset)
	{
		return new Token(category, type, offset, value, data);
	}

	@Override
	public String toString()
	{
		return String.format("Token(%s, %s, %s, %s, %s)", category, type, value, offset, data);
	}
}
