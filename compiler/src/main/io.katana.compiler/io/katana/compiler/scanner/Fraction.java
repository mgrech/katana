package io.katana.compiler.scanner;

import java.math.BigInteger;
import java.util.Objects;

public class Fraction
{
	public final BigInteger numerator;
	public final BigInteger denominator;

	private Fraction(BigInteger numerator, BigInteger denominator)
	{
		this.numerator = numerator;
		this.denominator = denominator;
	}

	public static Fraction of(BigInteger numerator, BigInteger denominator)
	{
		return new Fraction(numerator, denominator);
	}

	public static Fraction of(String numerator, String denominator)
	{
		return of(new BigInteger(numerator), new BigInteger(denominator));
	}

	public static Fraction of(long numerator, long denominator)
	{
		return of(Long.toString(numerator), Long.toString(denominator));
	}

	@Override
	public boolean equals(Object other)
	{
		if(!(other instanceof Fraction))
			return false;

		Fraction fraction = (Fraction)other;

		return Objects.equals(numerator, fraction.numerator) &&
		       Objects.equals(denominator, fraction.denominator);
	}

	@Override
	public String toString()
	{
		return String.format("Fraction(%s, %s)", numerator, denominator);
	}
}
