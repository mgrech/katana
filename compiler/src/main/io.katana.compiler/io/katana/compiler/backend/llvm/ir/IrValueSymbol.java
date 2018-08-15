package io.katana.compiler.backend.llvm.ir;

public class IrValueSymbol extends IrValue
{
	private final String name;

	public IrValueSymbol(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return '@' + name;
	}
}
