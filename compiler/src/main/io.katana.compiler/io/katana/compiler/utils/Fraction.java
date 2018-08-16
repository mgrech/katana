// Copyright 2018 Markus Grech
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

package io.katana.compiler.utils;

import java.math.BigInteger;
import java.util.Objects;

public class Fraction implements Comparable<Fraction>
{
	// ((1 << 24) - 1) * (1 << 104) == (2^24 - 1) * 2^104
	public static final Fraction FLOAT_MAX = of(BigInteger.ONE.shiftLeft(24).subtract(BigInteger.ONE).multiply(BigInteger.ONE.shiftLeft(104)), BigInteger.ONE);

	// ((1 << 53) - 1) * (1 << 971) == (2^53 - 1) * 2^971
	public static final Fraction DOUBLE_MAX = of(BigInteger.ONE.shiftLeft(53).subtract(BigInteger.ONE).multiply(BigInteger.ONE.shiftLeft(971)), BigInteger.ONE);

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

	public Fraction negated()
	{
		return of(numerator.negate(), denominator);
	}

	public Fraction reduced()
	{
		var gcd = numerator.gcd(denominator);
		return of(numerator.divide(gcd), denominator.divide(gcd));
	}

	public float toFloat()
	{
		var reduced = reduced();
		return reduced.numerator.floatValue() / reduced.denominator.floatValue();
	}

	public double toDouble()
	{
		var reduced = reduced();
		return reduced.numerator.doubleValue() / reduced.denominator.doubleValue();
	}

	@Override
	public int compareTo(Fraction other)
	{
		var thisnum = numerator.multiply(other.denominator);
		var othernum = other.numerator.multiply(denominator);
		return thisnum.compareTo(othernum);
	}

	@Override
	public boolean equals(Object other)
	{
		if(!(other instanceof Fraction))
			return false;

		var fraction = (Fraction)other;

		return Objects.equals(numerator, fraction.numerator) &&
		       Objects.equals(denominator, fraction.denominator);
	}

	@Override
	public String toString()
	{
		return String.format("Fraction(%s, %s)", numerator, denominator);
	}
}
