// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.visitor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ReflectionUtils
{
	public static Method findMatchingMethod(Class clazz, String name, Class[] args)
	{
		var candidates = Arrays.stream(clazz.getDeclaredMethods())
		                       .filter(m -> m.getParameterCount() == args.length)
		                       .filter(m -> m.getName().equals(name))
		                       .filter(m -> isCallable(m, args))
		                       .collect(Collectors.toList());

		if(candidates.isEmpty())
		{
			var argsDesc = Arrays.stream(args)
			                     .map(a -> a == null ? "null" : '\'' + a.getName() + '\'')
			                     .collect(Collectors.joining(", "));

			var fmt = "no matching method found in class '%s' for argument(s) %s";
			throw new RuntimeException(String.format(fmt, clazz.getName(), argsDesc));
		}

		if(candidates.size() > 1)
			return tryFindExactMatch(clazz, candidates, args);

		return candidates.get(0);
	}

	private static boolean isCallable(Method method, Class[] args)
	{
		var params = method.getParameterTypes();

		return IntStream.range(0, args.length)
		                .allMatch(i -> args[i] == null || params[i].isAssignableFrom(args[i]));
	}

	private static Method tryFindExactMatch(Class clazz, List<Method> matches, Class[] args)
	{
		for(var method : matches)
		{
			var params = method.getParameterTypes();

			var exactMatch = IntStream.of(0, args.length)
			                          .allMatch(i -> args[i] == null || params[i] == args[i]);

			if(exactMatch)
				return method;
		}

		throw new RuntimeException(String.format("ambiguous method call in '%s'", clazz.getName()));
	}
}
