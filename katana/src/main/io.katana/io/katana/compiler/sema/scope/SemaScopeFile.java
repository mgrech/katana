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

package io.katana.compiler.sema.scope;

import io.katana.compiler.sema.SemaSymbol;

import java.util.*;

public class SemaScopeFile implements SemaScope
{
	private Map<String, List<SemaSymbol>> symbols = new HashMap<>();

	public void defineSymbol(SemaSymbol symbol)
	{
		String name = symbol.name();

		if(!symbols.containsKey(name))
			symbols.put(name, new ArrayList<>());

		symbols.get(name).add(symbol);
	}

	@Override
	public List<SemaSymbol> find(String name)
	{
		List<SemaSymbol> symbolList = symbols.get(name);
		return symbolList == null ? Collections.emptyList() : symbolList;
	}
}
