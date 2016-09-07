// Copyright 2016 Markus Grech
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

package katana.visitor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtils
{
	public static Method findMatchingMethod(Class clazz, String name, Class[] args)
	{
		Method[] allMethods = clazz.getDeclaredMethods();

		List<Method> sameNameAndParamCountMethods = new ArrayList<>();

		for(Method method : allMethods)
			if(method.getParameterCount() == args.length && method.getName().equals(name))
				sameNameAndParamCountMethods.add(method);

		List<Method> matches = new ArrayList<>();

		for(Method method : sameNameAndParamCountMethods)
			if(isCallable(method, args))
				matches.add(method);

		if(matches.isEmpty())
			throw new RuntimeException("no matching method found in " + clazz.getName());

		if(matches.size() > 1)
			throw new RuntimeException("ambiguous method call in " + clazz.getName());

		return matches.get(0);
	}

	public static boolean isCallable(Method method, Class[] args)
	{
		Class<?>[] params = method.getParameterTypes();

		for(int i = 0; i != args.length; ++i)
			if(args[i] != null && !params[i].isAssignableFrom(args[i]))
				return false;

		return true;
	}
}
