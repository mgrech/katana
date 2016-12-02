package katana.platform;

public enum Arch
{
	UNKNOWN("unknown", -1, -1),
	AMD64("x86_64", 8, 8),
	X86("x86", 4, 4);

	private final String value;
	public final int intSize;
	public final int pointerSize;

	Arch(String value, int intSize, int pointerSize)
	{
		this.value = value;
		this.intSize = intSize;
		this.pointerSize = pointerSize;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
