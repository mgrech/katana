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
import io.katana.compiler.sema.SemaSymbol;
import io.katana.compiler.sema.decl.SemaDeclOperator;
import io.katana.compiler.sema.scope.SemaScopeFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OperatorParser
{
	private static List<SemaDeclOperator> parseOpSeq(SemaScopeFile scope, String seq, Kind kind)
	{
		List<SemaDeclOperator> result = new ArrayList<>();

		while(!seq.isEmpty())
		{
			StringBuilder builder = new StringBuilder();
			builder.append(seq);

			while(builder.length() != 0)
			{
				String op = builder.toString();
				List<SemaSymbol> symbols = scope.find(Operator.declName(op, kind));

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
		List<SemaDeclOperator> ops = parseOpSeq(scope, seq.seq, Kind.PREFIX);

		for(int i = ops.size() - 1; i != -1; --i)
			seq.expr = createPrefixOp(seq.expr, ops.get(i));

		replace.accept(seq.expr);
	}

	private static AstExpr createPostfixOp(AstExpr expr, SemaDeclOperator decl)
	{
		return new AstExprOpPostfix(expr, decl);
	}

	private static void replacePostfixOpSeq(AstExprOpPostfixSeq seq, Consumer<AstExpr> replace, SemaScopeFile scope)
	{
		List<SemaDeclOperator> ops = parseOpSeq(scope, seq.seq, Kind.POSTFIX);

		for(int i = 0; i != ops.size(); ++i)
			seq.expr = createPostfixOp(seq.expr, ops.get(i));

		replace.accept(seq.expr);
	}

	private static List<SemaDeclOperator> findInfixOperators(SemaScopeFile scope, List<String> symbols)
	{
		List<SemaDeclOperator> operators = new ArrayList<>();

		for(String symbol : symbols)
		{
			List<SemaSymbol> candidates = scope.find(Operator.declName(symbol, Kind.INFIX));

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
		Associativity assoc = ops.get(0).operator.associativity;

		for(int i = 1; i != ops.size(); ++i)
			if(ops.get(i).operator.associativity != assoc)
				return false;

		return true;
	}

	private static List<List<Object>> split(List<Object> list, List<Integer> splitPoints)
	{
		List<List<Object>> result = new ArrayList<>();

		int previousSplitPoint = -1;

		for(int splitPoint : splitPoints)
		{
			result.add(list.subList(previousSplitPoint + 1, splitPoint));
			previousSplitPoint = splitPoint;
		}

		result.add(list.subList(previousSplitPoint + 1, list.size()));
		return result;
	}

	private static List<Integer> lowestPrecedenceIndices(List<Object> expr)
	{
		List<Integer> result = new ArrayList<>();

		int precedence = ((SemaDeclOperator)expr.get(1)).operator.precedence;
		result.add(1);

		for(int i = 3; i != expr.size(); i += 2)
		{
			SemaDeclOperator op = (SemaDeclOperator)expr.get(i);

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

		for(SemaDeclOperator op : ops)
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

		List<Integer> opIndices = lowestPrecedenceIndices(expr);

		List<SemaDeclOperator> ops = opIndices.stream()
		                                      .map(expr::get)
		                                      .map(o -> (SemaDeclOperator)o)
		                                      .collect(Collectors.toList());

		Associativity associativity = checkAssoc(ops);

		List<AstExpr> children = split(expr, opIndices).stream()
		                                               .map(OperatorParser::parse)
		                                               .collect(Collectors.toList());

		if(associativity == Associativity.RIGHT)
		{
			AstExpr result = children.get(children.size() - 1);

			for(int i = children.size() - 2; i != -1; --i)
				result = createInfixOp(children.get(i), result, ops.get(i));

			return result;
		}

		AstExpr result = children.get(0);

		for(int i = 1; i != children.size(); ++i)
			result = createInfixOp(result, children.get(i), ops.get(i - 1));

		return result;
	}

	private static void replaceInfixOpList(AstExprOpInfixList list, Consumer<AstExpr> replace, SemaScopeFile scope)
	{
		List<SemaDeclOperator> ops = findInfixOperators(scope, list.ops);
		List<Object> expr = new ArrayList<>();

		for(int i = 0; i != ops.size(); ++i)
		{
			expr.add(list.exprs.get(i));
			expr.add(ops.get(i));
		}

		expr.add(list.exprs.get(list.exprs.size() - 1));

		AstExpr replacement = parse(expr);
		replace.accept(replacement);
	}

	public static void replace(LateParseExprs list, SemaScopeFile scope)
	{
		for(Map.Entry<AstExprOpPrefixSeq, Consumer<AstExpr>> entry : list.prefixSeqs.entrySet())
			replacePrefixOpSeq(entry.getKey(), entry.getValue(), scope);

		for(Map.Entry<AstExprOpPostfixSeq, Consumer<AstExpr>> entry : list.postfixSeqs.entrySet())
			replacePostfixOpSeq(entry.getKey(), entry.getValue(), scope);

		for(Map.Entry<AstExprOpInfixList, Consumer<AstExpr>> entry : list.infixLists.entrySet())
			replaceInfixOpList(entry.getKey(), entry.getValue(), scope);
	}
}
