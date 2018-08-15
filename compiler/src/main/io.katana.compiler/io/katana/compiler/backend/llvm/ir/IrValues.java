package io.katana.compiler.backend.llvm.ir;

import java.util.List;

public class IrValues
{
	public static final IrValueConstant UNDEF = new IrValueConstant("undef");

	public static final IrValueConstant NULL = new IrValueConstant("null");
	public static final IrValueConstant ADDRESS_ONE = new IrValueConstant("inttoptr (i8 1 to i8*)");

	public static final IrValueConstant TRUE = new IrValueConstant("true");
	public static final IrValueConstant FALSE = new IrValueConstant("false");

	public static IrValueConstant ofConstant(boolean value)
	{
		return value ? TRUE : FALSE;
	}
	public static IrValueConstant ofConstant(long value)
	{
		return new IrValueConstant("" + value);
	}

	public static IrValueConstant ofConstant(double value)
	{
		var bits = Double.doubleToRawLongBits(value);
		var literal = String.format("0x%x", bits);
		return new IrValueConstant(literal);
	}

	public static IrValueConstant ofConstantArray(String elementType, List<IrValue> elements)
	{
		var builder = new StringBuilder();

		if(!elements.isEmpty())
		{
			builder.append(elementType);
			builder.append(' ');
			builder.append(elements.get(0));

			for(var i = 1; i != elements.size(); ++i)
			{
				builder.append(", ");
				builder.append(elementType);
				builder.append(' ');
				builder.append(elements.get(i));
			}
		}

		var literal = String.format("[%s]", builder.toString());
		return new IrValueConstant(literal);
	}

	public static IrValueSsa ofSsa(String name)
	{
		return new IrValueSsa(name);
	}

	public static IrValueSymbol ofSymbol(String name)
	{
		return new IrValueSymbol(name);
	}
}
