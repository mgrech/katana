// Copyright 2016-2018 Markus Grech
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

package io.katana.compiler.analysis;

import io.katana.compiler.BuiltinType;
import io.katana.compiler.ast.expr.AstExpr;
import io.katana.compiler.ast.stmt.*;
import io.katana.compiler.backend.PlatformContext;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.diag.TypeString;
import io.katana.compiler.sema.decl.SemaDecl;
import io.katana.compiler.sema.decl.SemaDeclDefinedFunction;
import io.katana.compiler.sema.expr.*;
import io.katana.compiler.sema.scope.SemaScopeFunction;
import io.katana.compiler.sema.stmt.*;
import io.katana.compiler.sema.type.SemaTypeBuiltin;
import io.katana.compiler.utils.Maybe;
import io.katana.compiler.visitor.IVisitor;

import java.util.IdentityHashMap;
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

	public void validateGotoTargets()
	{
		for(var entry : gotos.entrySet())
		{
			var labelName = entry.getValue();
			var label = function.labels.get(labelName);

			if(label == null)
				throw new CompileException(String.format("unknown label '%s'", labelName));

			entry.getKey().target = label;
		}
	}

	private SemaStmt visit(AstStmtCompound compound)
	{
		var semaCompound = new SemaStmtCompound();

		for(var stmt : compound.body)
			semaCompound.body.add(validate(stmt));

		return semaCompound;
	}

	private SemaStmt visit(AstStmtLabel label)
	{
		var semaLabel = new SemaStmtLabel(label.name);

		if(!function.defineLabel(semaLabel))
			throw new CompileException(String.format("duplicate label name '%s'", label.name));

		return semaLabel;
	}

	private SemaStmt visit(AstStmtGoto goto_)
	{
		var semaGoto = new SemaStmtGoto(null);
		gotos.put(semaGoto, goto_.label);
		return semaGoto;
	}

	private enum ConditionKind
	{
		IF,
		WHILE,
	}

	private SemaExpr validateCondition(AstExpr expr, ConditionKind kind)
	{
		var condition = ExprValidator.validate(expr, scope, context, validateDecl, Maybe.some(SemaTypeBuiltin.BOOL));

		if(!Types.isBuiltin(condition.type(), BuiltinType.BOOL))
		{
			var fmt = "%s requires condition of type 'bool', got '%s'";
			var gotten = TypeString.of(Types.removeConst(condition.type()));
			throw new CompileException(String.format(fmt, kind.toString().toLowerCase(), gotten));
		}

		return condition;
	}

	private SemaStmt visit(AstStmtIf if_)
	{
		var condition = validateCondition(if_.condition, ConditionKind.IF);
		return new SemaStmtIf(if_.negated, condition, validate(if_.then));
	}

	private SemaStmt visit(AstStmtIfElse ifelse)
	{
		var condition = validateCondition(ifelse.condition, ConditionKind.IF);
		return new SemaStmtIfElse(ifelse.negated, condition, validate(ifelse.then), validate(ifelse.else_));
	}

	private SemaStmt visit(AstStmtLoop loop)
	{
		return new SemaStmtLoop(validate(loop.body));
	}

	private SemaStmt visit(AstStmtWhile while_)
	{
		var condition = validateCondition(while_.condition, ConditionKind.WHILE);
		return new SemaStmtWhile(while_.negated, condition, validate(while_.body));
	}

	private SemaStmt visit(AstStmtReturn return_)
	{
		var value = return_.value.map(retval -> ExprValidator.validate(retval, scope, context, validateDecl, Maybe.some(function.ret)));
		var retTypeNoConst = Types.removeConst(function.ret);

		if(value.isNone())
		{
			if(Types.isVoid(retTypeNoConst))
				return new SemaStmtReturn(Maybe.none());

			var fmt = "function '%s' returns value of type '%s', no value given";
			throw new CompileException(String.format(fmt, function.qualifiedName(), TypeString.of(retTypeNoConst)));
		}

		var type = value.unwrap().type();
		var typeNoConst = Types.removeConst(type);

		if(!Types.equal(retTypeNoConst, typeNoConst))
		{
			var fmt = "function '%s' returns value of type '%s', '%s' given";
			throw new CompileException(String.format(fmt, function.qualifiedName(), TypeString.of(retTypeNoConst), TypeString.of(typeNoConst)));
		}

		return new SemaStmtReturn(value);
	}

	private SemaStmt visit(AstStmtExprStmt exprStmt)
	{
		var expr = ExprValidator.validate(exprStmt.expr, scope, context, validateDecl, Maybe.none());
		return new SemaStmtExprStmt(expr);
	}

	private SemaStmt visit(AstStmtVar var_)
	{
		var maybeDeclaredType = var_.type.map(type -> TypeValidator.validate(type, scope, context, validateDecl));
		var maybeDeclaredTypeNoConst = maybeDeclaredType.map(Types::removeConst);

		if(var_.init.isNone())
		{
			if(maybeDeclaredType.isNone())
				throw new CompileException(String.format("variable '%s' with 'undef' initializer has no explicit type", var_.name));

			if(!function.defineVariable(var_.name, maybeDeclaredType.unwrap()))
				throw new CompileException(String.format("redefinition of variable '%s'", var_.name));

			return new SemaStmtNullStmt();
		}

		var init = ExprValidator.validate(var_.init.unwrap(), scope, context, validateDecl, maybeDeclaredTypeNoConst);
		var initTypeNoConst = Types.removeConst(init.type());
		var varType = maybeDeclaredType.or(initTypeNoConst);
		var varTypeNoConst = Types.removeConst(varType);

		if(!Types.equal(varTypeNoConst, initTypeNoConst))
		{
			String fmt = "initializer for variable '%s' has wrong type: expected '%s', got '%s'";
			throw new CompileException(String.format(fmt, var_.name, TypeString.of(varTypeNoConst), TypeString.of(initTypeNoConst)));
		}

		if(!function.defineVariable(var_.name, varType))
			throw new CompileException(String.format("redefinition of variable '%s'", var_.name));

		if(init.kind() == ExprKind.LVALUE)
			init = new SemaExprImplicitConversionLValueToRValue(init);

		var semaVar = function.variablesByName.get(var_.name);
		var varRef = new SemaExprNamedVar(semaVar);
		return new SemaStmtExprStmt(new SemaExprAssign(varRef, init));
	}

	private SemaStmt visit(AstStmtUnreachable unreachable)
	{
		return SemaStmtUnreachable.INSTANCE;
	}
}
