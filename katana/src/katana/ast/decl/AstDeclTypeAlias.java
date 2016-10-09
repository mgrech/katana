package katana.ast.decl;

import katana.ast.type.AstType;

public class AstDeclTypeAlias extends AstDecl
{
	public AstDeclTypeAlias(boolean exported, String name, AstType type)
	{
		super(exported, false);
		this.name = name;
		this.type = type;
	}

	public String name;
	public AstType type;
}
