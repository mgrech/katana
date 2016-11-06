package katana.ast.decl;

import katana.ast.stmt.AstStmt;
import katana.ast.type.AstType;
import katana.op.Kind;
import katana.op.Operator;
import katana.utils.Maybe;

import java.util.List;

public class AstDeclDefinedOperator extends AstDeclDefinedFunction
{
	public AstDeclDefinedOperator(boolean exported, boolean opaque, String op, Kind kind, List<Param> params, Maybe<AstType> ret, List<AstStmt> body)
	{
		super(exported, opaque, Operator.implName(op, kind), params, ret, body);
		this.op = op;
		this.kind = kind;
	}

	public final String op;
	public final Kind kind;
}
