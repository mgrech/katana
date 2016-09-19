// Copyright 2016 Markus Grech
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

package katana.sema;

import katana.backend.PlatformContext;
import katana.sema.decl.Decl;
import katana.sema.decl.Function;
import katana.sema.expr.Assign;
import katana.sema.expr.Expr;
import katana.sema.expr.NamedLocal;
import katana.sema.stmt.*;
import katana.sema.type.Builtin;
import katana.sema.type.Type;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class StmtValidator implements IVisitor
{
	private IdentityHashMap<Goto, String> gotos = new IdentityHashMap<>();
	private Function function;
	private FunctionScope scope;
	private PlatformContext context;
	private Consumer<Decl> validateDecl;

	public StmtValidator(Function function, FunctionScope scope, PlatformContext context, Consumer<Decl> validateDecl)
	{
		this.function = function;
		this.scope = scope;
		this.context = context;
		this.validateDecl = validateDecl;
	}

	public Stmt validate(katana.ast.stmt.Stmt stmt)
	{
		return (Stmt)stmt.accept(this);
	}

	public void finalizeValidation()
	{
		for(Map.Entry<Goto, String> entry : gotos.entrySet())
		{
			String labelName = entry.getValue();
			Label label = function.labels.get(labelName);

			if(label == null)
				throw new RuntimeException("unknown label '@" + labelName + "'");

			entry.getKey().target = label;
		}
	}

	private Stmt visit(katana.ast.stmt.Compound compound)
	{
		Compound semaCompound = new Compound();

		for(katana.ast.stmt.Stmt stmt : compound.body)
			semaCompound.body.add(validate(stmt));

		return semaCompound;
	}

	private Stmt visit(katana.ast.stmt.Label label)
	{
		Label semaLabel = new Label(label.name);

		if(!function.defineLabel(semaLabel))
			throw new RuntimeException("duplicate label name '" + label.name + "'");

		return semaLabel;
	}

	private Stmt visit(katana.ast.stmt.Goto goto_)
	{
		Goto semaGoto = new Goto(null);
		gotos.put(semaGoto, goto_.label);
		return semaGoto;
	}

	private Stmt visit(katana.ast.stmt.If if_)
	{
		Expr condition = ExprValidator.validate(if_.condition, scope, context, validateDecl, Maybe.some(Builtin.BOOL));

		if(condition.type().isNone() || !Type.same(condition.type().unwrap(), Builtin.BOOL))
			throw new RuntimeException("if requires condition of type bool");

		return new If(if_.negated, condition, validate(if_.then));
	}

	private Stmt visit(katana.ast.stmt.IfElse ifelse)
	{
		Expr condition = ExprValidator.validate(ifelse.condition, scope, context, validateDecl, Maybe.some(Builtin.BOOL));

		if(condition.type().isNone() || !Type.same(condition.type().unwrap(), Builtin.BOOL))
			throw new RuntimeException("if requires condition of type bool");

		return new IfElse(ifelse.negated, condition, validate(ifelse.then), validate(ifelse.else_));
	}

	private Stmt visit(katana.ast.stmt.Loop loop)
	{
		return new Loop(validate(loop.body));
	}

	private Stmt visit(katana.ast.stmt.While while_)
	{
		Expr condition = ExprValidator.validate(while_.condition, scope, context, validateDecl, Maybe.some(Builtin.BOOL));

		if(condition.type().isNone() || !Type.same(condition.type().unwrap(), Builtin.BOOL))
			throw new RuntimeException("while requires condition of type bool");

		return new While(while_.negated, condition, validate(while_.body));
	}

	private Stmt visit(katana.ast.stmt.Return return_)
	{
		Maybe<Expr> value = return_.value.map((v) -> ExprValidator.validate(v, scope, context, validateDecl, function.ret));

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

	private Stmt visit(katana.ast.stmt.ExprStmt exprStmt)
	{
		Expr expr = ExprValidator.validate(exprStmt.expr, scope, context, validateDecl, Maybe.none());
		return new ExprStmt(expr);
	}

	private Stmt visit(katana.ast.stmt.VarDef varDef)
	{
		String name = varDef.name;
		Expr init = ExprValidator.validate(varDef.init, scope, context, validateDecl, Maybe.none());
		Maybe<Type> maybeType = init.type();

		if(maybeType.isNone())
			throw new RuntimeException(String.format("initializer for var '%s' yields void", name));

		if(!function.defineLocal(name, maybeType.unwrap()))
			throw new RuntimeException(String.format("redefinition of variable '%s'", name));

		Function.Local local = function.localsByName.get(name);
		NamedLocal localref = new NamedLocal(local);
		localref.useAsLValue(true);
		return new ExprStmt(new Assign(localref, init));
	}
}
