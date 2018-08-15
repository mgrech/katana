package io.katana.compiler.backend.llvm.ir;

public class IrValueSsa extends IrValue
{
	private final String name;

	public IrValueSsa(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return '%' + name;
	}
}
