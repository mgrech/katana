package katana;

import java.util.function.Function;
import java.util.function.Supplier;

public class Maybe<T>
{
	private static final Maybe NONE = new Maybe<>(null);

	private Maybe(T value)
	{
		this.value = value;
	}

	public boolean isNone()
	{
		return value == null;
	}

	public boolean isSome()
	{
		return value != null;
	}

	public T get()
	{
		if(isNone())
			throw new NullPointerException();

		return unwrap();
	}

	public T unwrap()
	{
		return value;
	}

	public <U> Maybe<U> map(Function<T, U> f)
	{
		if(isNone())
			return none();

		return wrap(f.apply(value));
	}

	public static <T> Maybe<T> none()
	{
		return NONE;
	}

	public static <T> Maybe<T> some(T value)
	{
		if(value == null)
			throw new NullPointerException();

		return new Maybe<>(value);
	}

	public static <T> Maybe<T> wrap(T value)
	{
		if(value == null)
			return none();

		return new Maybe<>(value);
	}

	public T or(T value)
	{
		return this.value == null ? value : this.value;
	}

	public T or(Supplier<T> func)
	{
		return this.value == null ? value : func.get();
	}

	private T value;
}
