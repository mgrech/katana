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
