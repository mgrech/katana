package io.katana.compiler.ast.type;

public class AstTypeSlice extends AstType
{
	public AstType type;

	public AstTypeSlice(AstType type)
	{
		this.type = type;
	}
}
