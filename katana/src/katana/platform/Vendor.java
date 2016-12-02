package katana.platform;

public enum Vendor
{
	UNKNOWN("unknown"),
	APPLE("apple"),
	PC("pc");

	private final String value;

	Vendor(String value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
