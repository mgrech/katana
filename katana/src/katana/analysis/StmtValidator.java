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

package katana.analysis;

import katana.ast.stmt.*;
import katana.backend.PlatformContext;
import katana.sema.decl.SemaDecl;
import katana.sema.decl.SemaDeclDefinedFunction;
import katana.sema.expr.SemaExpr;
import katana.sema.expr.SemaExprAssign;
import katana.sema.expr.SemaExprNamedLocal;
import katana.sema.scope.SemaScopeFunction;
import katana.sema.stmt.*;
import katana.sema.type.SemaType;
import katana.sema.type.SemaTypeBuiltin;
import katana.utils.Maybe;
import katana.visitor.IVisitor;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class StmtValidator implements IVisitor
{
	private IdentityHashMap<SemaStmtGoto, String> gotos = new IdentityHashMap<>();
	private SemaDeclDefinedFunction function;
	private SemaScopeFunction scope;
	private PlatformContext context;
	private Consumer<SemaDecl> validateDecl;

	public StmtValidator(SemaDeclDefinedFunction function, SemaScopeFunction scope, PlatformContext context, Consumer<SemaDecl> validateDecl)
	{
		this.function = function;
		this.scope = scope;
		this.context = context;
		this.validateDecl = validateDecl;
	}

	public SemaStmt validate(AstStmt stmt)
	{
		return (SemaStmt)stmt.accept(this);
	}

	public void finalizeValidation()
	{
		for(Map.Entry<SemaStmtGoto, String> entry : gotos.entrySet())
		{
			String labelName = entry.getValue();
			SemaStmtLabel label = function.labels.get(labelName);

			if(label == null)
				throw new RuntimeException("unknown label '@" + labelName + "'");

			entry.getKey().target = label;
		}
	}

	private SemaStmt visit(AstStmtCompound compound)
	{
		SemaStmtCompound semaCompound = new SemaStmtCompound();

		for(AstStmt stmt : compound.body)
			semaCompound.body.add(validate(stmt));

		return semaCompound;
	}

	private SemaStmt visit(AstStmtLabel label)
	{
		SemaStmtLabel semaLabel = new SemaStmtLabel(label.name);

		if(!function.defineLabel(semaLabel))
			throw new RuntimeException("duplicate label name '" + label.name + "'");

		return semaLabel;
	}

	private SemaStmt visit(AstStmtGoto goto_)
	{
		SemaStmtGoto semaGoto = new SemaStmtGoto(null);
		gotos.put(semaGoto, goto_.label);
		return semaGoto;
	}

	private SemaStmt visit(AstStmtIf if_)
	{
		SemaExpr condition = ExprValidator.validate(if_.condition, scope, context, validateDecl, Maybe.some(SemaTypeBuiltin.BOOL));

		if(condition.type().isNone() || !SemaType.same(condition.type().unwrap(), SemaTypeBuiltin.BOOL))
			throw new RuntimeException("if requires condition of type bool");

		return new SemaStmtIf(if_.negated, condition, validate(if_.then));
	}

	private SemaStmt visit(AstStmtIfElse ifelse)
	{
		SemaExpr condition = ExprValidator.validate(ifelse.condition, scope, context, validateDecl, Maybe.some(SemaTypeBuiltin.BOOL));

		if(condition.type().isNone() || !SemaType.same(condition.type().unwrap(), SemaTypeBuiltin.BOOL))
			throw new RuntimeException("if requires condition of type bool");

		return new SemaStmtIfElse(ifelse.negated, condition, validate(ifelse.then), validate(ifelse.else_));
	}

	private SemaStmt visit(AstStmtLoop loop)
	{
		return new SemaStmtLoop(validate(loop.body));
	}

	private SemaStmt visit(AstStmtWhile while_)
	{
		SemaExpr condition = ExprValidator.validate(while_.condition, scope, context, validateDecl, Maybe.some(SemaTypeBuiltin.BOOL));

		if(condition.type().isNone() || !SemaType.same(condition.type().unwrap(), SemaTypeBuiltin.BOOL))
			throw new RuntimeException("while requires condition of type bool");

		return new SemaStmtWhile(while_.negated, condition, validate(while_.body));
	}

	private SemaStmt visit(AstStmtReturn return_)
	{
		Maybe<SemaExpr> value = return_.value.map(retval -> ExprValidator.validate(retval, scope, context, validateDecl, function.ret));

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
			Maybe<SemaType> maybeType = value.unwrap().type();

			if(maybeType.isNone())
			{
				String fmt = "function %s returns value of type %s, expression given results in no value";
				throw new RuntimeException(String.format(fmt, function.qualifiedName(), function.ret.unwrap()));
			}

			SemaType type = maybeType.unwrap();
			SemaType typeDecayed = TypeHelper.decay(type);

			if(!SemaType.same(function.ret.unwrap(), typeDecayed))
			{
				String fmt = "function %s returns value of type %s, %s given";
				throw new RuntimeException(String.format(fmt, function.qualifiedName(), function.ret.unwrap(), typeDecayed));
			}
		}

		return new SemaStmtReturn(value);
	}

	private SemaStmt visit(AstStmtExprStmt exprStmt)
	{
		SemaExpr expr = ExprValidator.validate(exprStmt.expr, scope, context, validateDecl, Maybe.none());
		return new SemaStmtExprStmt(expr);
	}

	private SemaStmt visit(AstStmtLocal local)
	{
		Maybe<SemaType> maybeDeclaredType = local.type.map(type -> TypeValidator.validate(type, scope, context, validateDecl));
		Maybe<SemaType> maybeDeclaredTypeDecayed = maybeDeclaredType.map(TypeHelper::decay);
		SemaExpr init = ExprValidator.validate(local.init, scope, context, validateDecl, maybeDeclaredTypeDecayed);

		if(init.type().isNone())
			throw new RuntimeException(String.format("initializer for local '%s' yields void", local.name));

		SemaType initType = init.type().unwrap();
		SemaType initTypeDecayed = TypeHelper.decay(initType);
		SemaType localType = maybeDeclaredType.or(initTypeDecayed);
		SemaType localTypeDecayed = TypeHelper.decay(localType);

		if(!SemaType.same(localTypeDecayed, initTypeDecayed))
		{
			String fmt = "initializer for local '%s' has wrong type: expected '%s', got '%s'";
			throw new RuntimeException(String.format(fmt, local.name, localTypeDecayed, initTypeDecayed));
		}

		if(!function.defineLocal(local.name, localType))
			throw new RuntimeException(String.format("redefinition of local '%s'", local.name));

		SemaDeclDefinedFunction.Local semaLocal = function.localsByName.get(local.name);
		SemaExprNamedLocal localref = new SemaExprNamedLocal(semaLocal);
		localref.useAsLValue(true);
		return new SemaStmtExprStmt(new SemaExprAssign(localref, init));
	}
}
