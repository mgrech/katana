package katana.platform;

public enum Environment
{
	UNKNOWN("unknown"),
	GNU("gnu"),
	MSVC("msvc");

	private final String value;

	Environment(String value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
