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

public class ScannerState implements Cloneable
{
	public int line = 1;
	public int currentOffset = 0;
	public int tokenOffset = 0;
	public int currentColumn = 1;
	public int tokenColumn = 1;

	public Token token = Tokens.BEGIN;

	public ScannerState() {}

	private ScannerState(int line, int currentOffset, int tokenOffset, int currentColumn, int tokenColumn, Token token)
	{
		this.line = line;
		this.currentOffset = currentOffset;
		this.tokenOffset = tokenOffset;
		this.currentColumn = currentColumn;
		this.tokenColumn = tokenColumn;
		this.token = token;
	}

	@Override
	public ScannerState clone()
	{
		return new ScannerState(line, currentOffset, tokenOffset, currentColumn, tokenColumn, token);
	}
}
