package katana.sema;

import katana.Maybe;
import katana.backend.PlatformContext;
import katana.sema.decl.Function;
import katana.sema.stmt.*;
import katana.sema.type.Builtin;
import katana.visitor.IVisitor;

@SuppressWarnings("unused")
public class StmtValidator implements IVisitor
{
	private StmtValidator() {}

	private Stmt visit(katana.ast.stmt.Compound compound, Function function, PlatformContext context)
	{
		Compound semaCompound = new Compound();

		for(katana.ast.Stmt stmt : compound.body)
			semaCompound.body.add(validate(stmt, function, context));

		return semaCompound;
	}

	private Stmt visit(katana.ast.stmt.Label label, Function function, PlatformContext context)
	{
		Label semaLabel = new Label(label.name);

		if(!function.defineLabel(semaLabel))
			throw new RuntimeException("duplicate label name '" + label.name + "'");

		return semaLabel;
	}

	private Stmt visit(katana.ast.stmt.Goto goto_, Function function, PlatformContext context)
	{
		Label label = function.labels.get(goto_.label);

		if(label == null)
			throw new RuntimeException("unknown label '@" + goto_.label + "'");

		return new Goto(label);
	}

	public Stmt visit(katana.ast.stmt.If if_, Function function, PlatformContext context)
	{
		Expr condition = ExprValidator.validate(if_.condition, function, context);

		if(condition.type().isNone() || !Type.same(condition.type().unwrap(), Builtin.BOOL))
			throw new RuntimeException("if requires condition of type bool");

		Stmt then = validate(if_.then, function, context);
		return new If(if_.negated, condition, then);
	}

	public Stmt visit(katana.ast.stmt.IfElse ifelse, Function function, PlatformContext context)
	{
		Expr condition = ExprValidator.validate(ifelse.condition, function, context);

		if(condition.type().isNone() || !Type.same(condition.type().unwrap(), Builtin.BOOL))
			throw new RuntimeException("if requires condition of type bool");

		Stmt then = validate(ifelse.then, function, context);
		Stmt else_ = validate(ifelse.else_, function, context);
		return new IfElse(ifelse.negated, condition, then, else_);
	}

	public Stmt visit(katana.ast.stmt.Loop loop, Function function, PlatformContext fcontext)
	{
		return new Loop(validate(loop.body, function, fcontext));
	}

	public Stmt visit(katana.ast.stmt.While while_, Function function, PlatformContext context)
	{
		Expr condition = ExprValidator.validate(while_.condition, function, context);

		if(condition.type().isNone() || !Type.same(condition.type().unwrap(), Builtin.BOOL))
			throw new RuntimeException("while requires condition of type bool");

		Stmt body = validate(while_.body, function, context);
		return new While(while_.negated, condition, body);
	}

	public Stmt visit(katana.ast.stmt.Return return_, Function function, PlatformContext context)
	{
		Maybe<Expr> value = return_.value.map((v) -> ExprValidator.validate(v, function, context));

		if(function.ret.isNone() && value.isSome())
		{
			String fmt = "function %s returns nothing, value given";
			throw new RuntimeException(String.format(fmt, function.qualifiedName()));
		}

		if(function.ret.isSome() && value.isNone())
		{
			String fmt = "function %s returns value of type %s, none given";
			throw new RuntimeException(String.format(fmt, function.qualifiedName(), function.ret.unwrap()));
		}

		if(function.ret.isSome() && value.isSome())
		{
			Maybe<Type> maybeType = value.unwrap().type();

			if(maybeType.isNone())
			{
				String fmt = "function %s returns value of type %s, expression given results in no value";
				throw new RuntimeException(String.format(fmt, function.qualifiedName(), function.ret.unwrap()));
			}

			Type type = maybeType.unwrap();

			if(!Type.same(function.ret.unwrap(), type))
			{
				String fmt = "function %s returns value of type %s, %s given";
				throw new RuntimeException(String.format(fmt, function.qualifiedName(), function.ret.unwrap(), type));
			}
		}

		return new Return(value);
	}

	public Stmt visit(katana.ast.stmt.ExprStmt exprStmt, Function function, PlatformContext context)
	{
		Expr expr = ExprValidator.validate(exprStmt.expr, function, context);
		return new ExprStmt(expr);
	}

	public static Stmt validate(katana.ast.Stmt stmt, Function function, PlatformContext context)
	{
		StmtValidator validator = new StmtValidator();
		return (Stmt)stmt.accept(validator, function, context);
	}
}
