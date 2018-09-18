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

import io.katana.compiler.utils.Rethrow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ReflectionUtils
{
	public static Method[] findVisitMethods(Class clazz)
	{
		return Arrays.stream(clazz.getDeclaredMethods())
		             .filter(m -> m.getName().equals("visit"))
		             .peek(m -> m.setAccessible(true))
		             .toArray(Method[]::new);
	}

	private static Method resolve(Method[] methods, Object... args)
	{
		for(var method : methods)
		{
			if(method.getParameterCount() != args.length)
				continue;

			var params = method.getParameterTypes();

			if(!IntStream.range(0, args.length)
			             .allMatch(i -> args[i] == null || params[i].isAssignableFrom(args[i].getClass())))
				continue;

			return method;
		}

		return null;
	}

	private static void noMatchingMethodFound(IVisitor self, Object... args)
	{
		var argsDesc = Arrays.stream(args)
		                     .map(a -> a == null ? "null" : '\'' + a.getClass().getName() + '\'')
		                     .collect(Collectors.joining(", "));

		var fmt = "no matching method found in class '%s' for argument(s) %s";
		throw new RuntimeException(String.format(fmt, self.getClass().getName(), argsDesc));
	}

	public static Object resolveAndInvoke(Method[] methods, IVisitor self, Object... args)
	{
		var method = resolve(methods, args);

		if(method == null)
			noMatchingMethodFound(self, args);

		try
		{
			return method.invoke(self, args);
		}
		catch(IllegalAccessException ex)
		{
			throw new RuntimeException(ex);
		}
		catch(InvocationTargetException ex)
		{
			Rethrow.of(ex.getTargetException());
		}

		throw new AssertionError("unreachable");
	}
}
