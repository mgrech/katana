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

package io.katana.compiler.op;

import io.katana.compiler.ast.LateParseExprs;
import io.katana.compiler.ast.expr.*;
import io.katana.compiler.diag.CompileException;
import io.katana.compiler.sema.decl.SemaDeclOperator;
import io.katana.compiler.sema.scope.SemaScopeFile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OperatorParser
{
	private static List<SemaDeclOperator> parseOpSeq(SemaScopeFile scope, String seq, Kind kind)
	{
		var result = new ArrayList<SemaDeclOperator>();

		while(!seq.isEmpty())
		{
			var builder = new StringBuilder();
			builder.append(seq);

			while(builder.length() != 0)
			{
				var op = builder.toString();
				var symbols = scope.find(Operator.declName(op, kind));

				if(symbols.size() == 1)
				{
					result.add((SemaDeclOperator)symbols.get(0));
					seq = seq.substring(op.length());
					break;
				}
				else if(symbols.size() > 1)
					throw new CompileException(String.format("multiple definitions for operator '%s %s'", kind.toString().toLowerCase(), op));

				builder.deleteCharAt(builder.length() - 1);
			}

			if(builder.length() == 0)
				throw new CompileException(String.format("operator '%s' could not be found", seq));
		}

		return result;
	}

	private static AstExpr createPrefixOp(AstExpr expr, SemaDeclOperator decl)
	{
		if(decl.operator.symbol.equals("&"))
			return new AstExprAddressof(expr);

		if(decl.operator.symbol.equals("*"))
			return new AstExprDeref(expr);

		if(decl.operator.symbol.equals("-") && expr instanceof AstExprLitInt)
			return new AstExprLitInt(((AstExprLitInt)expr).value.negate(), ((AstExprLitInt)expr).type);

		return new AstExprOpPrefix(expr, decl);
	}

	private static void replacePrefixOpSeq(AstExprOpPrefixSeq seq, Consumer<AstExpr> replace, SemaScopeFile scope)
	{
		var ops = parseOpSeq(scope, seq.symbols, Kind.PREFIX);

		for(var i = ops.size() - 1; i != -1; --i)
			seq.nestedExpr = createPrefixOp(seq.nestedExpr, ops.get(i));

		replace.accept(seq.nestedExpr);
	}

	private static AstExpr createPostfixOp(AstExpr expr, SemaDeclOperator decl)
	{
		return new AstExprOpPostfix(expr, decl);
	}

	private static void replacePostfixOpSeq(AstExprOpPostfixSeq seq, Consumer<AstExpr> replace, SemaScopeFile scope)
	{
		var ops = parseOpSeq(scope, seq.symbols, Kind.POSTFIX);

		for(var i = 0; i != ops.size(); ++i)
			seq.nestedExpr = createPostfixOp(seq.nestedExpr, ops.get(i));

		replace.accept(seq.nestedExpr);
	}

	private static List<SemaDeclOperator> findInfixOperators(SemaScopeFile scope, List<String> symbols)
	{
		var operators = new ArrayList<SemaDeclOperator>();

		for(var symbol : symbols)
		{
			var candidates = scope.find(Operator.declName(symbol, Kind.INFIX));

			if(candidates.isEmpty())
				throw new CompileException(String.format("operator 'infix %s' could not be found", symbol));

			if(candidates.size() > 1)
				throw new CompileException(String.format("multiple definitions for operator 'infix %s'", symbol));

			operators.add((SemaDeclOperator)candidates.get(0));
		}

		return operators;
	}

	private static AstExpr createInfixOp(AstExpr left, AstExpr right, SemaDeclOperator decl)
	{
		if(decl.operator.symbol.equals("="))
			return new AstExprAssign(left, right);

		return new AstExprOpInfix(left, right, decl);
	}

	private static boolean sameAssociativity(List<SemaDeclOperator> ops)
	{
		var assoc = ops.get(0).operator.associativity;

		for(var i = 1; i != ops.size(); ++i)
			if(ops.get(i).operator.associativity != assoc)
				return false;

		return true;
	}

	private static List<List<Object>> split(List<Object> list, List<Integer> splitPoints)
	{
		var result = new ArrayList<List<Object>>();

		var previousSplitPoint = -1;

		for(var splitPoint : splitPoints)
		{
			result.add(list.subList(previousSplitPoint + 1, splitPoint));
			previousSplitPoint = splitPoint;
		}

		result.add(list.subList(previousSplitPoint + 1, list.size()));
		return result;
	}

	private static List<Integer> lowestPrecedenceIndices(List<Object> expr)
	{
		var result = new ArrayList<Integer>();

		var precedence = ((SemaDeclOperator)expr.get(1)).operator.precedence;
		result.add(1);

		for(var i = 3; i != expr.size(); i += 2)
		{
			var op = (SemaDeclOperator)expr.get(i);

			if(op.operator.precedence == precedence)
				result.add(i);
			else if(op.operator.precedence < precedence)
			{
				result.clear();
				result.add(i);
				precedence = op.operator.precedence;
			}
		}

		return result;
	}

	private static Associativity checkAssoc(List<SemaDeclOperator> ops)
	{
		if(ops.size() == 0)
			throw new AssertionError("unreachable");

		if(ops.size() == 1)
			return ops.get(0).operator.associativity;

		for(var op : ops)
			if(op.operator.associativity == Associativity.NONE)
				throw new CompileException(String.format("operator 'infix %s' is non-associative", op.operator.symbol));

		if(!sameAssociativity(ops))
			throw new CompileException("operators of same precedence used within an expression must also have the same associativity");

		return ops.get(0).operator.associativity;
	}

	private static AstExpr parse(List<Object> expr)
	{
		if(expr.size() % 2 == 0)
			throw new AssertionError("unreachable");

		if(expr.size() == 1)
			return (AstExpr)expr.get(0);

		if(expr.size() == 3)
			return createInfixOp((AstExpr)expr.get(0), (AstExpr)expr.get(2), (SemaDeclOperator)expr.get(1));

		var opIndices = lowestPrecedenceIndices(expr);

		var ops = opIndices.stream()
		                   .map(expr::get)
		                   .map(o -> (SemaDeclOperator)o)
		                   .collect(Collectors.toList());

		var associativity = checkAssoc(ops);

		var children = split(expr, opIndices).stream()
		                                     .map(OperatorParser::parse)
		                                     .collect(Collectors.toList());

		if(associativity == Associativity.RIGHT)
		{
			var result = children.get(children.size() - 1);

			for(var i = children.size() - 2; i != -1; --i)
				result = createInfixOp(children.get(i), result, ops.get(i));

			return result;
		}

		var result = children.get(0);

		for(var i = 1; i != children.size(); ++i)
			result = createInfixOp(result, children.get(i), ops.get(i - 1));

		return result;
	}

	private static void replaceInfixOpList(AstExprOpInfixList list, Consumer<AstExpr> replace, SemaScopeFile scope)
	{
		var ops = findInfixOperators(scope, list.infixOps);
		var expr = new ArrayList<>();

		for(var i = 0; i != ops.size(); ++i)
		{
			expr.add(list.nestedExprs.get(i));
			expr.add(ops.get(i));
		}

		expr.add(list.nestedExprs.get(list.nestedExprs.size() - 1));

		var replacement = parse(expr);
		replace.accept(replacement);
	}

	public static void replace(LateParseExprs list, SemaScopeFile scope)
	{
		for(var entry : list.prefixSeqs.entrySet())
			replacePrefixOpSeq(entry.getKey(), entry.getValue(), scope);

		for(var entry : list.postfixSeqs.entrySet())
			replacePostfixOpSeq(entry.getKey(), entry.getValue(), scope);

		for(var entry : list.infixLists.entrySet())
			replaceInfixOpList(entry.getKey(), entry.getValue(), scope);
	}
}
