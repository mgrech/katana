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

package katana.op;

import katana.ast.DelayedExprParseList;
import katana.ast.expr.*;
import katana.sema.SemaSymbol;
import katana.sema.decl.SemaDeclOperator;
import katana.sema.scope.SemaScopeFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
					throw new RuntimeException(String.format("multiple definitions for operator '%s %s'", kind.toString().toLowerCase(), op));

				builder.deleteCharAt(builder.length() - 1);
			}

			if(builder.length() == 0)
				throw new RuntimeException(String.format("operator '%s' could not be found", seq));
		}

		return result;
	}

	private static AstExpr createPrefixOp(AstExpr expr, SemaDeclOperator decl)
	{
		if(decl.operator.symbol.equals("&"))
			return new AstExprAddressof(expr);

		if(decl.operator.symbol.equals("*"))
			return new AstExprDeref(expr);

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

	private static List<SemaDeclOperator> findOperators(SemaScopeFile scope, List<String> symbols)
	{
		List<SemaDeclOperator> operators = new ArrayList<>();

		for(String symbol : symbols)
		{
			List<SemaSymbol> candidates = scope.find(Operator.declName(symbol, Kind.INFIX));

			if(candidates.isEmpty())
				throw new RuntimeException(String.format("operator 'infix %s' could not be found", symbol));

			if(candidates.size() > 1)
				throw new RuntimeException(String.format("multiple definitions for operator 'infix %s'", symbol));

			operators.add((SemaDeclOperator)candidates.get(0));
		}

		return operators;
	}

	private static class IndexRange
	{
		public int beginIncl;
		public int endExcl;

		public IndexRange(int beginIncl, int endExcl)
		{
			this.beginIncl = beginIncl;
			this.endExcl = endExcl;
		}
	}

	private static void splitAddRange(List<IndexRange> ranges, int beginIncl, int endExcl)
	{
		if(beginIncl == endExcl)
			ranges.add(new IndexRange(beginIncl, beginIncl));
		else
			ranges.add(new IndexRange(beginIncl + 1, endExcl));
	}

	private static List<IndexRange> splitIndexRange(IndexRange range, List<Integer> splitPointsExcl)
	{
		if(splitPointsExcl.isEmpty())
			throw new AssertionError("splitIndexRange: no split points");

		List<IndexRange> ranges = new ArrayList<>();
		splitAddRange(ranges, range.beginIncl, splitPointsExcl.get(0));

		for(int i = 0; i != splitPointsExcl.size() - 1; ++i)
			splitAddRange(ranges, splitPointsExcl.get(i), splitPointsExcl.get(i + 1));

		splitAddRange(ranges, splitPointsExcl.get(splitPointsExcl.size() - 1), range.endExcl);

		return ranges;
	}

	private static boolean sameAssociativity(List<SemaDeclOperator> ops, List<Integer> indices)
	{
		Associativity assoc = ops.get(indices.get(0)).operator.associativity;

		for(int i = 1; i != indices.size(); ++i)
			if(ops.get(indices.get(i)).operator.associativity != assoc)
				return false;

		return true;
	}

	private static AstExpr createInfixOp(AstExpr left, AstExpr right, SemaDeclOperator decl)
	{
		if(decl.operator.symbol.equals("="))
			return new AstExprAssign(left, right);

		return new AstExprOpInfix(left, right, decl);
	}

	private static AstExpr parse(List<AstExpr> exprs, List<SemaDeclOperator> ops, IndexRange range)
	{
		if(range.beginIncl == range.endExcl)
			return exprs.get(range.beginIncl);

		if(range.beginIncl + 1 == range.endExcl)
			return createInfixOp(exprs.get(range.beginIncl), exprs.get(range.endExcl), ops.get(range.beginIncl));

		int lowestPrec = IntStream.range(range.beginIncl, range.endExcl)
		                          .mapToObj(ops::get)
		                          .min((a, b) -> a.operator.precedence - b.operator.precedence)
		                          .get().operator.precedence;

		List<Integer> lowestPrecIdxs = IntStream.range(range.beginIncl, range.endExcl)
		                                        .filter(i -> ops.get(i).operator.precedence == lowestPrec)
		                                        .mapToObj(i -> i)
		                                        .collect(Collectors.toList());

		if(lowestPrecIdxs.size() > 1)
		{
			for(int idx : lowestPrecIdxs)
				if(ops.get(idx).operator.associativity == Associativity.NONE)
					throw new RuntimeException(String.format("operator 'infix %s' is non-associative", ops.get(idx).operator.symbol));

			if(!sameAssociativity(ops, lowestPrecIdxs))
				throw new RuntimeException("operators of same precedence used within an expression must also have the same associativity");
		}

		List<AstExpr> children = splitIndexRange(range, lowestPrecIdxs).stream()
		                                                               .map(r ->parse(exprs, ops, r))
		                                                               .collect(Collectors.toList());

		boolean leftAssociative = ops.get(lowestPrecIdxs.get(0)).operator.associativity == Associativity.LEFT;

		AstExpr expr = leftAssociative ? children.get(0) : children.get(children.size() - 1);

		if(leftAssociative)
			for(int i = 1; i != children.size(); ++i)
				expr = createInfixOp(expr, children.get(i), ops.get(lowestPrecIdxs.get(i - 1)));

		else
			for(int i = children.size() - 2; i != -1; --i)
				expr = createInfixOp(children.get(i), expr, ops.get(lowestPrecIdxs.get(i)));

		return expr;
	}

	private static void replaceInfixOpList(AstExprOpInfixList list, Consumer<AstExpr> replace, SemaScopeFile scope)
	{
		List<SemaDeclOperator> ops = findOperators(scope, list.ops);
		AstExpr replacement = parse(list.exprs, ops, new IndexRange(0, ops.size()));
		replace.accept(replacement);
	}

	public static void replace(DelayedExprParseList list, SemaScopeFile scope)
	{
		for(Map.Entry<AstExprOpPrefixSeq, Consumer<AstExpr>> entry : list.prefixSeqs.entrySet())
			replacePrefixOpSeq(entry.getKey(), entry.getValue(), scope);

		for(Map.Entry<AstExprOpPostfixSeq, Consumer<AstExpr>> entry : list.postfixSeqs.entrySet())
			replacePostfixOpSeq(entry.getKey(), entry.getValue(), scope);

		for(Map.Entry<AstExprOpInfixList, Consumer<AstExpr>> entry : list.infixLists.entrySet())
			replaceInfixOpList(entry.getKey(), entry.getValue(), scope);
	}
}
