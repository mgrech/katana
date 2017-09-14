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

package katana.testing.scanner.tests;

import katana.scanner.Token;
import katana.testing.scanner.ScannerTests;
import katana.testing.scanner.Utils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(ScannerTests.class)
public class WhitespaceTest
{
	@Test
	public void skipsWhitespaceCharacters()
	{
		List<Token> tokens = Utils.tokenizeString("\t\r a");
		assertTrue(tokens.size() == 1);
		assertEquals(tokens.get(0).value, "a");
		assertEquals(tokens.get(0).offset, 3);
	}

	@Test
	public void skipsComments()
	{
		List<Token> tokens = Utils.tokenizeString("#foo\na");
		assertEquals(tokens.size(), 1);
		assertEquals(tokens.get(0).value, "a");
		assertEquals(tokens.get(0).offset, 5);
	}

	@Test
	public void skipsLineBreaks()
	{
		List<Token> tokens = Utils.tokenizeString("a\nb");
		assertEquals(tokens.size(), 2);

		assertEquals(tokens.get(0).value, "a");
		assertEquals(tokens.get(0).offset, 0);

		assertEquals(tokens.get(1).value, "b");
		assertEquals(tokens.get(1).offset, 2);
	}
}
