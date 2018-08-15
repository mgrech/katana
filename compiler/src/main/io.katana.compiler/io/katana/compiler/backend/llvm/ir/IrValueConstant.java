package io.katana.compiler.backend.llvm.ir;

public class IrValueConstant extends IrValue
{
	private final String value;

	public IrValueConstant(String value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
