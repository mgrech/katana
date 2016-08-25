package katana.visitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface IVisitable
{
	default Object accept(IVisitor visitor, Object... args)
	{
		Class[] classes = new Class[args.length + 1];
		classes[0] = getClass();

		for(int i = 0; i != args.length; ++i)
			classes[i + 1] = args[i].getClass();

		Object[] invoke = new Object[args.length + 1];
		invoke[0] = this;
		System.arraycopy(args, 0, invoke, 1, args.length);

		try
		{
			Method visit = ReflectionUtils.findMatchingMethod(visitor.getClass(), "visit", classes);
			visit.setAccessible(true);
			return visit.invoke(visitor, invoke);
		}

		catch(IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}

		catch(InvocationTargetException e)
		{
			Throwable throwable = e.getTargetException();

			try
			{
				throw (RuntimeException)throwable;
			}

			catch(ClassCastException ex)
			{
				throw new RuntimeException(throwable);
			}
		}
	}
}