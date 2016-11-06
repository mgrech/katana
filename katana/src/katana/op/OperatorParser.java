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
import katana.ast.expr.AstExpr;
import katana.ast.expr.AstExprOpInfixList;
import katana.ast.expr.AstExprOpPostfixSeq;
import katana.ast.expr.AstExprOpPrefixSeq;
import katana.sema.SemaSymbol;
import katana.sema.scope.SemaScopeFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OperatorParser
{
	private static List<String> parseOpSeq(SemaScopeFile scope, String seq, Kind kind)
	{
		List<String> result = new ArrayList<>();

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
					result.add(op);
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

	private static void replacePrefixOpSeq(AstExprOpPrefixSeq seq, Consumer<AstExpr> replace, SemaScopeFile scope)
	{
		List<String> ops = parseOpSeq(scope, seq.seq, Kind.PREFIX);

		for(int i = ops.size() - 1; i != -1; --i)
			seq.expr = new AstExprOpPrefix(ops.get(i), seq.expr);

		replace.accept(seq.expr);
	}

	private static void replacePostfixOpSeq(AstExprOpPostfixSeq seq, Consumer<AstExpr> replace, SemaScopeFile scope)
	{
		List<String> ops = parseOpSeq(scope, seq.seq, Kind.POSTFIX);

		for(int i = 0; i != ops.size(); ++i)
			seq.expr = new AstExprOpPostfix(seq.expr, ops.get(i));

		replace.accept(seq.expr);
	}

	private static void replaceInfixOpList(AstExprOpInfixList list, Consumer<AstExpr> replace, SemaScopeFile scope)
	{
		throw new RuntimeException("nyi");
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
