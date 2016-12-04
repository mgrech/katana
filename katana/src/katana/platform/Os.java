package katana.platform;

public enum Os
{
	UNKNOWN("unknown"),
	LINUX("linux"),
	MACOS("darwin"),
	WINDOWS("windows");

	private final String value;

	Os(String value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
