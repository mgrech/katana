package io.katana.compiler.sema.type;

public class SemaTypeSlice extends SemaType
{
	public SemaType type;

	public SemaTypeSlice(SemaType type)
	{
		this.type = type;
	}
}
