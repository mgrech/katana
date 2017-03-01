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
	public int begin = 0;
	public int end = 0;
	public Token token = Tokens.BEGIN;

	public ScannerState() {}

	private ScannerState(int begin, int end, Token token)
	{
		this.begin = begin;
		this.end = end;
		this.token = token;
	}

	@Override
	public ScannerState clone()
	{
		return new ScannerState(begin, end, token);
	}
}
