package katana.platform;

import java.math.BigInteger;

public enum Arch
{
	UNKNOWN("unknown", -1, -1),
	AMD64("x86_64", 8, 8),
	X86("x86", 4, 4);

	private final String value;
	public final BigInteger intSize;
	public final BigInteger pointerSize;

	Arch(String value, int intSize, int pointerSize)
	{
		this.value = value;
		this.intSize = BigInteger.valueOf(intSize);
		this.pointerSize = BigInteger.valueOf(pointerSize);
	}

	@Override
	public String toString()
	{
		return value;
	}
}
